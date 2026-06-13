package com.coduel.flow;

import com.coduel.api.MatchParticipantApi;
import com.coduel.api.MatchApi;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.GameMode;
import com.coduel.model.constant.MatchState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(rollbackFor = ApiException.class)
public class MatchFlow {

    @Autowired
    private MatchApi matchApi;
    @Autowired
    private MatchParticipantApi matchParticipantApi;

    // Creates an ACTIVE match and enrolls its players in one transaction (two Apis → a real Flow).
    public Match create(GameMode gameMode, Long problemId, List<Long> userIds) throws ApiException {
        Match saved = matchApi.add(ConversionHelper.toMatch(gameMode, problemId, MatchState.ACTIVE));
        for (Long userId : userIds) {
            matchParticipantApi.add(ConversionHelper.toParticipant(saved.getId(), userId));
        }
        return saved;
    }
}
