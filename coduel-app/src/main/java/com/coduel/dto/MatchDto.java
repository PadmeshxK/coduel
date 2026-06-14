package com.coduel.dto;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import com.coduel.model.data.MatchData;
import com.coduel.model.data.MatchParticipantData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MatchDto {

    @Autowired
    private UserApi userApi;
    @Autowired
    private MatchApi matchApi;
    @Autowired
    private MatchParticipantApi matchParticipantApi;
    @Autowired
    private ProblemApi problemApi;

    // Read-only assembly for the duel UI: match + its problem + both participants' profiles.
    public MatchData getMatch(Long matchId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Match match = matchApi.getCheckById(matchId);
        List<MatchParticipant> participants = matchParticipantApi.getByMatchId(matchId);

        // Only a match's own participants may view it.
        boolean isParticipant = participants.stream().anyMatch(p -> p.getUserId().equals(userId));
        if (!isParticipant) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_112, List.of(matchId));
        }

        List<MatchParticipantData> participantData = new ArrayList<>();
        for (MatchParticipant participant : participants) {
            participantData.add(
                    ConversionHelper.toMatchParticipantData(userApi.getCheckById(participant.getUserId())));
        }

        return ConversionHelper.toMatchData(
                match, problemApi.getCheckById(match.getProblemId()), participantData);
    }
}
