package com.coduel.flow;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.UserApi;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Problem;
import com.coduel.interfaces.MatchmakingQueue;
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

@Component
@Log4j2
@Transactional(rollbackFor = ApiException.class)
public class MatchmakingFlow {

    @Autowired
    private UserApi userApi;
    @Autowired
    private MatchmakingQueue matchmakingQueue;
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

    public MatchmakingResult join(String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();

        // Already in a duel (re-click, or paired while we were away) -> return it; never double-match.
        Match existing = findActiveMatch(userId);
        if (Objects.nonNull(existing)) {
            return matched(existing);
        }

        Problem problem = problemApi.getCheckRandomProblem();
        Long opponent = matchmakingQueue.poll();
        // No one waiting, we polled ourselves, or the polled player is a stale ghost already in a
        // match -> just wait.
        if (Objects.isNull(opponent) || opponent.equals(userId) || Objects.nonNull(findActiveMatch(opponent))) {
            matchmakingQueue.enqueue(userId);
            log.info("Matchmaking: user {} waiting (polled opponent={})", userId, opponent);
            return new MatchmakingResult(MatchmakingStatus.WAITING, null, null);
        }

        Match match = matchFlow.create(GameMode.DUEL, null, problem.getId(), List.of(opponent, userId));
        // both players must actually show up within the start grace, else the no-show forfeits.
        matchPresenceService.scheduleStartDeadline(match.getId());
        log.info("Matchmaking: matched user {} vs {} -> match {}", opponent, userId, match.getId());
        return new MatchmakingResult(MatchmakingStatus.MATCHED, match, problem.getSlug());
    }

    public MatchmakingResult status(String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Match existing = findActiveMatch(userId);
        return Objects.nonNull(existing)
                ? matched(existing)
                : new MatchmakingResult(MatchmakingStatus.WAITING, null, null);
    }

    // Cancel: drop the user from the queue (e.g. they navigated away while searching). No-op if absent.
    public void leave(String googleId) throws ApiException {
        matchmakingQueue.remove(userApi.getCheckByGoogleId(googleId).getId());
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

    private MatchmakingResult matched(Match match) throws ApiException {
        String slug = problemApi.getCheckById(match.getProblemId()).getSlug();
        return new MatchmakingResult(MatchmakingStatus.MATCHED, match, slug);
    }
}
