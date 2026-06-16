package com.coduel.dto;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
import com.coduel.flow.MatchFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.MatchEventPublisher;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.MatchEndReason;
import com.coduel.model.data.MatchData;
import com.coduel.model.data.MatchParticipantData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    @Autowired
    private MatchFlow matchFlow;
    @Autowired
    private MatchEventPublisher matchEventPublisher;

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

    // A participant gives up: the opponent wins. Idempotent via matchFlow.finish (no-op if already
    // ended), so a forfeit racing a real result can't double-decide the match.
    public void forfeit(Long matchId, String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        List<MatchParticipant> participants = matchParticipantApi.getByMatchId(matchId);

        boolean isParticipant = participants.stream().anyMatch(p -> p.getUserId().equals(userId));
        if (!isParticipant) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_112, List.of(matchId));
        }

        Long opponentId = participants.stream()
                .map(MatchParticipant::getUserId)
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);

        if (Objects.nonNull(opponentId) && matchFlow.finish(matchId, opponentId, MatchEndReason.OPPONENT_FORFEIT)) {
            matchEventPublisher.publish(matchId,
                    ConversionHelper.toMatchOverEvent(opponentId, MatchEndReason.OPPONENT_FORFEIT));
        }
    }
}
