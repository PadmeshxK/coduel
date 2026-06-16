package com.coduel.flow;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.SubmissionApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.Submission;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.MatchState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@Transactional(rollbackFor = ApiException.class)
public class SubmissionFlow {

    @Autowired
    private SubmissionApi submissionApi;
    @Autowired
    private MatchParticipantApi matchParticipantApi;
    @Autowired
    private MatchApi matchApi;

    public Submission create(Submission submission) throws ApiException {
        // A user may only submit into a match they belong to (matchId is client-supplied).
        if (Objects.nonNull(submission.getMatchId())) {
            checkParticipant(submission.getMatchId(), submission.getUserId());
        }
        return submissionApi.add(submission);
    }

    public Long getMatchWinner(Long matchId)  throws ApiException {
        Match currentMatch = matchApi.getCheckById(matchId);
        if(currentMatch.getState() != MatchState.ACTIVE){
            return null;
        }

        Submission firstSubmission = submissionApi.findFirstAcceptedSubmission(matchId);
        if(Objects.nonNull(firstSubmission)){
            return firstSubmission.getUserId();
        }
        return null;
    }

    private void checkParticipant(Long matchId, Long userId) throws ApiException {
        boolean participant = matchParticipantApi.getByMatchId(matchId).stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (!participant) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_112, List.of(matchId));
        }
    }
}
