package com.coduel.flow;

import com.coduel.api.LeaderboardApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.MatchApi;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.entity.Submission;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchEndReason;
import com.coduel.model.constant.MatchState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    // Creates an ACTIVE match and enrolls its players in one transaction (two Apis → a real Flow).
    public Match create(GameMode gameMode, Long problemId, List<Long> userIds) throws ApiException {
        Match saved = matchApi.add(ConversionHelper.toMatch(gameMode, problemId, MatchState.ACTIVE));
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
}
