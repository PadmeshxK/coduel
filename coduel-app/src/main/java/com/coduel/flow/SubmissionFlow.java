package com.coduel.flow;

import com.coduel.api.MatchParticipantApi;
import com.coduel.api.SubmissionApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Submission;
import com.coduel.model.constant.Errors;
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

    public Submission create(Submission submission) throws ApiException {
        // A user may only submit into a match they belong to (matchId is client-supplied).
        if (Objects.nonNull(submission.getMatchId())) {
            checkParticipant(submission.getMatchId(), submission.getUserId());
        }
        return submissionApi.add(submission);
    }

    private void checkParticipant(Long matchId, Long userId) throws ApiException {
        boolean participant = matchParticipantApi.getByMatchId(matchId).stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (!participant) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_112, List.of(matchId));
        }
    }
}
