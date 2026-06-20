package com.coduel.flow;

import com.coduel.api.LeaderboardApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.MatchApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.User;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchEndReason;
import com.coduel.model.constant.MatchState;
import com.coduel.model.result.ForfeitResult;
import com.coduel.model.result.MatchDetailResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Transactional(rollbackFor = ApiException.class)
public class MatchFlow {

    @Autowired
    private MatchApi matchApi;
    @Autowired
    private MatchParticipantApi matchParticipantApi;
    @Autowired
    private LeaderboardApi leaderboardApi;
    @Autowired
    private UserApi userApi;
    @Autowired
    private ProblemApi problemApi;

    // Creates an ACTIVE match and enrolls its players in one transaction (two Apis → a real Flow).
    // roomId is null for matchmaking duels, set for a match spawned from a persistent private room.
    public Match create(GameMode gameMode, Long roomId, Long problemId, List<Long> userIds) throws ApiException {
        Match match = ConversionHelper.toMatch(gameMode, problemId, MatchState.ACTIVE);
        match.setRoomId(roomId);
        Match saved = matchApi.add(match);
        for (Long userId : userIds) {
            matchParticipantApi.add(ConversionHelper.toParticipant(saved.getId(), userId));
        }
        return saved;
    }

    // Finishes a match and records the leaderboard outcome in the same transaction.
    // Returns true only on the ACTIVE -> FINISHED transition, so callers can publish exactly once.
    public boolean finish(Long matchId, Long winnerUserId, MatchEndReason reason) throws ApiException {
        if (!matchApi.markFinished(matchId, winnerUserId, reason)) {
            return false;
        }
        for (MatchParticipant participant : matchParticipantApi.getByMatchId(matchId)) {
            if (Objects.equals(participant.getUserId(), winnerUserId)) {
                leaderboardApi.recordWin(participant.getUserId());
            } else {
                leaderboardApi.recordLoss(participant.getUserId());
            }
        }
        return true;
    }

    // Read assembly for the duel UI: match + problem + both participants' profiles, with access check.
    public MatchDetailResult getMatchDetailResult(Long matchId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Match match = matchApi.getCheckById(matchId);
        List<MatchParticipant> participants = matchParticipantApi.getByMatchId(matchId);
        requireParticipant(participants, userId, matchId);

        Map<Long, User> profiles = new HashMap<>();
        for (MatchParticipant participant : participants) {
            profiles.put(participant.getUserId(), userApi.getCheckById(participant.getUserId()));
        }
        return new MatchDetailResult(match, problemApi.getCheckById(match.getProblemId()), participants, profiles);
    }

    // A participant gives up. The forfeiter drops out; if that leaves a single player standing, they
    // win (OPPONENT_FORFEIT) — otherwise the match plays on. Returns the forfeiter plus the winner
    // (null when the match continues), so the Dto can publish the right event.
    public ForfeitResult forfeit(Long matchId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        matchParticipantApi.forfeitUserByMatchIdAndUserId(matchId, userId);
        List<MatchParticipant> participants = matchParticipantApi.getByMatchId(matchId);

        List<Long> remaining = participants.stream()
                .filter(participant -> !participant.isForfeit())
                .map(MatchParticipant::getUserId)
                .toList();

        if (remaining.size() == 1 && finish(matchId, remaining.getFirst(), MatchEndReason.OPPONENT_FORFEIT)) {
            return new ForfeitResult(userId, remaining.getFirst());
        }
        return new ForfeitResult(userId, null);
    }

    private void requireParticipant(List<MatchParticipant> participants, Long userId, Long matchId)
            throws ApiException {
        if (participants.stream().noneMatch(p -> p.getUserId().equals(userId))) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_112, List.of(matchId));
        }
    }
}
