package com.coduel.dto;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.UserApi;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.flow.MatchFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.MatchmakingQueue;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchState;
import com.coduel.model.constant.MatchmakingStatus;
import com.coduel.model.data.MatchmakingData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class MatchmakingDto {

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

    public MatchmakingData join(String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Long opponent = matchmakingQueue.poll();
        // No one waiting (or only ourselves from a double-join) -> wait.
        if (Objects.isNull(opponent) || opponent.equals(userId)) {
            matchmakingQueue.enqueue(userId);
            return ConversionHelper.toMatchmakingData(MatchmakingStatus.WAITING, null);
        }
        Long problemId = problemApi.getRandomCheck().getId();
        Match match = matchFlow.create(GameMode.DUEL, problemId, List.of(opponent, userId));
        return ConversionHelper.toMatchmakingData(MatchmakingStatus.MATCHED, match);
    }

    public MatchmakingData status(String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        for (MatchParticipant participation : matchParticipantApi.getByUserId(userId)) {
            Match match = matchApi.getCheckById(participation.getMatchId());
            if (match.getState() == MatchState.ACTIVE) {
                return ConversionHelper.toMatchmakingData(MatchmakingStatus.MATCHED, match);
            }
        }
        return ConversionHelper.toMatchmakingData(MatchmakingStatus.WAITING, null);
    }
}
