package com.coduel.dto;

import com.coduel.common.exception.ApiException;
import com.coduel.flow.MatchmakingFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.MatchmakingData;
import com.coduel.model.result.MatchmakingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MatchmakingDto {

    @Autowired
    private MatchmakingFlow matchmakingFlow;

    public MatchmakingData join(String googleId) throws ApiException {
        return toData(matchmakingFlow.join(googleId));
    }

    public MatchmakingData status(String googleId) throws ApiException {
        return toData(matchmakingFlow.status(googleId));
    }

    public void leave(String googleId) throws ApiException {
        matchmakingFlow.leave(googleId);
    }

    private static MatchmakingData toData(MatchmakingResult result) {
        return ConversionHelper.toMatchmakingData(result.getStatus(), result.getMatch(), result.getSlug());
    }
}
