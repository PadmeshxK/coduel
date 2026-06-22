package com.coduel.dto;

import com.coduel.common.exception.ApiException;
import com.coduel.flow.MatchFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.MatchEventPublisher;
import com.coduel.model.constant.MatchEndReason;
import com.coduel.model.data.MatchData;
import com.coduel.model.data.MatchParticipantData;
import com.coduel.model.result.ForfeitResult;
import com.coduel.model.result.MatchDetailResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class MatchDto {

    @Autowired
    private MatchFlow matchFlow;
    @Autowired
    private MatchEventPublisher matchEventPublisher;

    public MatchData getMatch(Long matchId, String googleId) throws ApiException {
        MatchDetailResult view = matchFlow.getMatchDetailResult(matchId, googleId);
        List<MatchParticipantData> participants = view.getParticipants().stream()
                .map(p -> ConversionHelper.toMatchParticipantData(p, view.getProfiles().get(p.getUserId())))
                .toList();
        return ConversionHelper.toMatchData(view.getMatch(), view.getProblem(), participants);
    }

    public void forfeit(Long matchId, String googleId) throws ApiException {
        ForfeitResult result = matchFlow.forfeit(matchId, googleId);
        // Publish after the transactional finish — a WS event must not ride inside the DB transaction.
        // Always mark the forfeiter on every scoreboard first. In a 2-player duel the forfeit is the
        // terminal event, so without this the loser's row would never flip to "FORFEITED" — only the
        // win banner would show.
        matchEventPublisher.publish(matchId,
                ConversionHelper.toPlayerForfeitEvent(result.getForfeiterUserId()));
        if (Objects.nonNull(result.getWinnerUserId())) {
            // The forfeit left one player standing — they win.
            matchEventPublisher.publish(matchId,
                    ConversionHelper.toMatchOverEvent(result.getWinnerUserId(), MatchEndReason.OPPONENT_FORFEIT));
        }
    }
}
