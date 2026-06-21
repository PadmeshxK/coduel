package com.coduel.flow;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.UserApi;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Problem;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchState;
import com.coduel.model.constant.MatchmakingStatus;
import com.coduel.model.result.MatchmakingResult;
import com.coduel.websocket.MatchPresenceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * DB-side of matchmaking only. The waiting pool is a Redis queue owned by {@link com.coduel.dto.MatchmakingDto}:
 * it polls an opponent, calls {@link #pair}, and enqueues the caller when no duel was made.
 */
@Component
@Log4j2
@Transactional(rollbackFor = ApiException.class)
public class MatchmakingFlow {

    @Autowired
    private UserApi userApi;
    @Autowired
    private ProblemApi problemApi;
    @Autowired
    private MatchFlow matchFlow;
    @Autowired
    private MatchApi matchApi;
    @Autowired
    private MatchParticipantApi matchParticipantApi;
    @Autowired
    private MatchPresenceService matchPresenceService;

    public Long userIdOf(String googleId) throws ApiException {
        return userApi.getCheckByGoogleId(googleId).getId();
    }

    // Where the caller stands: already in an active duel (MATCHED) or not yet (WAITING). Always
    // carries the userId so the Dto can act on the queue without re-resolving it.
    public MatchmakingResult status(String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Match existing = findActiveMatch(userId);
        return Objects.nonNull(existing)
                ? matched(existing, userId)
                : new MatchmakingResult(MatchmakingStatus.WAITING, null, null, userId);
    }

    // Pair the caller with the opponent the Dto polled off the queue. MATCHED = a duel was created;
    // WAITING = no usable opponent (queue empty, polled self, or a ghost already in a match) -> the
    // Dto should enqueue the caller. A rejected opponent is simply dropped, which is what we want.
    public MatchmakingResult pair(Long userId, Long opponent) throws ApiException {
        if (Objects.isNull(opponent) || opponent.equals(userId) || Objects.nonNull(findActiveMatch(opponent))) {
            log.info("Matchmaking: user {} waiting (polled opponent={})", userId, opponent);
            return new MatchmakingResult(MatchmakingStatus.WAITING, null, null, userId);
        }
        Problem problem = problemApi.getCheckRandomProblem();
        Match match = matchFlow.create(GameMode.DUEL, null, problem.getId(), List.of(opponent, userId));
        // both players must actually show up within the start grace, else the no-show forfeits.
        matchPresenceService.scheduleStartDeadline(match.getId());
        log.info("Matchmaking: matched user {} vs {} -> match {}", opponent, userId, match.getId());
        return new MatchmakingResult(MatchmakingStatus.MATCHED, match, problem.getSlug(), userId);
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

    private MatchmakingResult matched(Match match, Long userId) throws ApiException {
        String slug = problemApi.getCheckById(match.getProblemId()).getSlug();
        return new MatchmakingResult(MatchmakingStatus.MATCHED, match, slug, userId);
    }
}
