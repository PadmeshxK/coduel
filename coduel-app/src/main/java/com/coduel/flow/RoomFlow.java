package com.coduel.flow;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.RoomApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Problem;
import com.coduel.entity.Room;
import com.coduel.entity.RoomMember;
import com.coduel.entity.User;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import com.coduel.model.data.RoomChatData;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchState;
import com.coduel.model.constant.RoomState;
import com.coduel.model.result.InviteResult;
import com.coduel.model.result.RoomDetailResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates the ephemeral private room (Redis-backed via RoomApi — NOT the DB). A room outlives its
 * matches: the host starts a match, it finishes, everyone returns, and the host can start another. The
 * roster is embedded in the Room aggregate; the first member (insertion order) is the host, so host
 * transfer on leave is implicit. Each roster mutation is followed by an explicit roomApi.save (there's
 * no JPA dirty-checking here). Not @Transactional — room state is Redis; the one DB write (match
 * creation in start()) runs in MatchFlow's own transaction.
 */
@Component
public class RoomFlow {

    // Fixed product cap — how many people can be in one room.
    public static final int MAX_ROOM_PLAYERS = 10;

    @Autowired
    private RoomApi roomApi;
    @Autowired
    private MatchFlow matchFlow;
    @Autowired
    private MatchApi matchApi;
    @Autowired
    private MatchParticipantApi matchParticipantApi;
    @Autowired
    private ProblemApi problemApi;
    @Autowired
    private UserApi userApi;

    // Open a new room; the creator is the first member (and thus the host).
    public Room create(String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Room room = ConversionHelper.toRoom();                       // fresh OPEN room, empty roster
        room.getMembers().add(ConversionHelper.toRoomMember(userId)); // creator = host
        return roomApi.save(room);                                    // assigns id + persists
    }

    // Returns both users so the Dto can push the invite notification post-action.
    public InviteResult invite(Long roomId, Long inviteeId, String googleId) throws ApiException {
        User requester = userApi.getCheckByGoogleId(googleId);
        Room room = roomApi.getCheckById(roomId);
        requireMember(room.getMembers(), requester.getId(), roomId);
        requireOpen(room);
        if (room.getMembers().size() >= MAX_ROOM_PLAYERS) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_116, List.of(MAX_ROOM_PLAYERS));
        }
        User invitee = userApi.getCheckById(inviteeId);
        return new InviteResult(requester, invitee);
    }

    // Add the caller to the room. Idempotent — re-joining (already a member) is a no-op. Authorization
    // (a held invite for this room) is enforced by the Dto before this is called; here it's pure roster
    // orchestration (open + capacity).
    public void join(Long roomId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Room room = roomApi.getCheckById(roomId);
        requireOpen(room);
        if (room.getMembers().stream().anyMatch(m -> m.getUserId().equals(userId))) {
            return; // already in — idempotent re-join
        }
        if (room.getMembers().size() >= MAX_ROOM_PLAYERS) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_116, List.of(MAX_ROOM_PLAYERS));
        }
        room.getMembers().add(ConversionHelper.toRoomMember(userId));
        roomApi.save(room);
    }

    // Whether the caller is already in the room — lets the Dto skip the invite gate for existing members
    // (a re-join after refresh has no fresh invite to consume).
    public boolean isMember(Long roomId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Room room = roomApi.getCheckById(roomId);
        return room.getMembers().stream().anyMatch(m -> m.getUserId().equals(userId));
    }

    // A non-host member toggles their lobby readiness. The host is implicitly ready (starting is
    // their signal), so a host call is a no-op.
    public void setReady(Long roomId, String googleId, boolean ready) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Room room = roomApi.getCheckById(roomId);
        List<RoomMember> members = room.getMembers();
        requireMember(members, userId, roomId);
        if (members.get(0).getUserId().equals(userId)) {
            return; // host doesn't ready up
        }
        members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .ifPresent(m -> m.setReady(ready));
        roomApi.save(room);
    }

    // Host starts a match for the current roster — only once every other member is ready (so nobody
    // is left stale in a finished match). Returns the new match id, and resets readiness for next time.
    public Long start(Long roomId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Room room = roomApi.getCheckById(roomId);
        List<RoomMember> members = room.getMembers();
        requireHost(members, userId); // only the host may start — throws ERR_117 otherwise
        requireOpen(room);
        validateActiveMatches(members);
        if (matchApi.findActiveByRoomId(roomId) != null) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_123, List.of());
        }
        if (members.size() < 2) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_118, List.of());
        }
        boolean everyoneReady = members.stream()
                .filter(m -> !m.getUserId().equals(userId))
                .allMatch(RoomMember::isReady);
        if (!everyoneReady) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_122, List.of());
        }
        Problem problem = problemApi.getCheckRandomProblem();
        List<Long> userIds = members.stream().map(RoomMember::getUserId).toList();
        Match match = matchFlow.create(GameMode.PRIVATE, roomId, problem.getId(), userIds);
        members.forEach(m -> m.setReady(false)); // clean slate for the next match
        roomApi.save(room);
        return match.getId();
    }

    // Full room view: room + roster profiles + host + the in-progress match (if any).
    public RoomDetailResult getView(Long roomId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Room room = roomApi.getCheckById(roomId);
        List<RoomMember> members = room.getMembers();
        requireMember(members, userId, roomId);

        Long hostId = members.get(0).getUserId();
        Map<Long, User> profiles = new HashMap<>();
        for (RoomMember m : members) {
            profiles.put(m.getUserId(), userApi.getCheckById(m.getUserId()));
        }
        Match active = matchApi.findActiveByRoomId(roomId);
        return new RoomDetailResult(room, members, profiles, hostId, userId, active == null ? null : active.getId());
    }

    // Leave the room (explicit action by the member).
    public boolean leave(Long roomId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        return removeMember(roomId, userId);
    }

    // Remove a member by userId — used by leave() and the disconnect/unsubscribe auto-kick. If they were
    // the last person the room is now empty and useless, so we delete it from Redis outright (the live
    // ROOM_CLOSED broadcast still drives the "room closed" screen for anyone who was watching). Otherwise
    // the next-oldest member becomes host implicitly. Returns true when the room was closed. No-op (false)
    // if the room is gone or they're no longer a member.
    public boolean removeMember(Long roomId, Long userId) throws ApiException {
        Room room = roomApi.findById(roomId);
        if (room == null) {
            return false;
        }
        List<RoomMember> members = room.getMembers();
        RoomMember mine = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst().orElse(null);
        if (mine == null) {
            return false;
        }
        if (members.get(0).getUserId().equals(userId) && members.size() == 1) {
            roomApi.delete(roomId); // last member left — the room is dead, so drop it from Redis entirely
            return true;
        }
        members.remove(mine);
        roomApi.save(room);
        return false;
    }

    // Build an ephemeral lobby-chat message from a member (membership-gated, body capped). Not
    // persisted — the Dto pushes it to the Redis ring buffer and broadcasts it.
    public RoomChatData composeChat(String googleId, Long roomId, String body) throws ApiException {
        User sender = userApi.getCheckByGoogleId(googleId);
        requireMember(roomApi.getCheckById(roomId).getMembers(), sender.getId(), roomId);
        String trimmed = body.strip();
        if (trimmed.length() > 1000) {
            trimmed = trimmed.substring(0, 1000);
        }
        return ConversionHelper.toRoomChatData(sender, trimmed);
    }

    // Membership gate for reading the lobby chat history.
    public void requireMembership(String googleId, Long roomId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        requireMember(roomApi.getCheckById(roomId).getMembers(), userId, roomId);
    }

    private void requireMember(List<RoomMember> members, Long userId, Long roomId) throws ApiException {
        if (members.stream().noneMatch(m -> m.getUserId().equals(userId))) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_121, List.of(roomId));
        }
    }

    private void requireHost(List<RoomMember> members, Long userId) throws ApiException {
        if (members.isEmpty() || !members.get(0).getUserId().equals(userId)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_117, List.of());
        }
    }

    private void requireOpen(Room room) throws ApiException {
        if (room.getState() != RoomState.OPEN) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_119, List.of());
        }
    }

    private Match findActiveMatch(Long userId) throws ApiException {
        for (MatchParticipant participation : matchParticipantApi.getByUserId(userId)) {
            Match match = matchApi.getCheckById(participation.getMatchId());
            if (match.getState() == MatchState.ACTIVE) {
                return match;
            }
        }
        return null;
    }

    private void validateActiveMatches(List<RoomMember> members) throws ApiException {
        for (RoomMember member : members) {
            Match match = findActiveMatch(member.getUserId());
            if (!Objects.isNull(match)) {
                throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_124, List.of());
            }
        }
    }
}
