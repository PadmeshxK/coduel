package com.coduel.flow;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.RoomApi;
import com.coduel.api.RoomMemberApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Problem;
import com.coduel.entity.Room;
import com.coduel.entity.RoomMember;
import com.coduel.entity.User;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchState;
import com.coduel.model.constant.RoomState;
import com.coduel.model.result.InviteResult;
import com.coduel.model.result.RoomDetailResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates the persistent private room. A room outlives its matches: the host starts a match,
 * it finishes, everyone returns to the room, and the host can start another (rematch). The oldest
 * member (lowest id) is the host; host transfer on leave is implicit via that ordering.
 */
@Component
@Transactional(rollbackFor = ApiException.class)
public class RoomFlow {

    // Fixed product cap — how many people can be in one room.
    public static final int MAX_ROOM_PLAYERS = 10;

    @Autowired
    private RoomApi roomApi;
    @Autowired
    private RoomMemberApi roomMemberApi;
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
        Room room = new Room();
        room.setState(RoomState.OPEN);
        Room saved = roomApi.add(room);
        roomMemberApi.add(member(saved.getId(), userId));
        return saved;
    }

    // Returns both users so the Dto can push the invite notification post-transaction.
    public InviteResult invite(Long roomId, Long inviteeId, String googleId) throws ApiException {
        User requester = userApi.getCheckByGoogleId(googleId);
        List<RoomMember> members = roomMemberApi.getByRoomId(roomId);
        requireMember(members, requester.getId(), roomId);
        requireOpen(roomApi.getCheckById(roomId));
        if (members.size() >= MAX_ROOM_PLAYERS) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_116, List.of(MAX_ROOM_PLAYERS));
        }
        User invitee = userApi.getCheckById(inviteeId);
        return new InviteResult(requester, invitee);
    }

    // Accept an invite / join the room. Idempotent — re-joining is a no-op.
    public void join(Long roomId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        requireOpen(roomApi.getCheckById(roomId));
        List<RoomMember> members = roomMemberApi.getByRoomId(roomId);
        if (members.size() >= MAX_ROOM_PLAYERS) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_116, List.of(MAX_ROOM_PLAYERS));
        }
        if (members.stream().noneMatch(m -> m.getUserId().equals(userId))) {
            roomMemberApi.add(member(roomId, userId));
        }
    }

    // A non-host member toggles their lobby readiness. The host is implicitly ready (starting is
    // their signal), so a host call is a no-op.
    public void setReady(Long roomId, String googleId, boolean ready) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        List<RoomMember> members = roomMemberApi.getByRoomId(roomId);
        requireMember(members, userId, roomId);
        if (members.get(0).getUserId().equals(userId)) {
            return; // host doesn't ready up
        }
        members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .ifPresent(m -> m.setReady(ready));
    }

    // Host starts a match for the current roster — only once every other member is ready (so nobody
    // is left stale in a finished match). Returns the new match id, and resets readiness for next time.
    public Long start(Long roomId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        List<RoomMember> members = roomMemberApi.getByRoomId(roomId);
        requireHost(members, userId); // only the host may start — throws ERR_117 otherwise
        requireOpen(roomApi.getCheckById(roomId));
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
        return match.getId();
    }

    // Full room view: room + roster profiles + host + the in-progress match (if any).
    public RoomDetailResult getView(Long roomId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Room room = roomApi.getCheckById(roomId);
        List<RoomMember> members = roomMemberApi.getByRoomId(roomId);
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

    // Remove a member by userId — used by leave() and the disconnect auto-kick. Closes the room if
    // the host was the last person; otherwise the next-oldest member becomes host implicitly.
    // Returns true when the room was closed. No-op (false) if they're no longer a member.
    public boolean removeMember(Long roomId, Long userId) throws ApiException {
        List<RoomMember> members = roomMemberApi.getByRoomId(roomId);
        RoomMember mine = members.stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst().orElse(null);
        if (mine == null) {
            return false;
        }
        if (members.get(0).getUserId().equals(userId) && members.size() == 1) {
            roomApi.getCheckById(roomId).setState(RoomState.CLOSED);
            return true;
        }
        roomMemberApi.delete(mine);
        return false;
    }

    private RoomMember member(Long roomId, Long userId) {
        RoomMember m = new RoomMember();
        m.setRoomId(roomId);
        m.setUserId(userId);
        return m;
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
        for(RoomMember member : members) {
            Match match = findActiveMatch(member.getUserId());
            if(!Objects.isNull(match)) {
                throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_124, List.of());
            }
        }
    }
}
