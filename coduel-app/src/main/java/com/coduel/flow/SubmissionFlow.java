package com.coduel.flow;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.SubmissionApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Match;
import com.coduel.entity.MatchParticipant;
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
    @Autowired
    private UserApi userApi;

    public Submission create(Submission submission, String googleId) throws ApiException {
        // Identity from the session, never the form.
        submission.setUserId(userApi.getCheckByGoogleId(googleId).getId());
        // A user may only submit into a match they belong to (matchId is client-supplied).
        if (Objects.nonNull(submission.getMatchId())) {
            checkParticipant(submission.getMatchId(), submission.getUserId());
        }
        return submissionApi.add(submission);
    }

    // Ownership-checked load: a submission carries its source code, so only the author may read it
    // (an opponent learns submissionIds from live match events). Not-found hides existence.
    public Submission getOwned(Long id, String googleId) throws ApiException {
        Submission submission = submissionApi.getCheckById(id);
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        if (!submission.getUserId().equals(userId)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_106, List.of(id));
        }
        return submission;
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
        MatchParticipant participant = matchParticipantApi.getByMatchId(matchId).stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        if (participant == null) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_112, List.of(matchId));
        }
        // A player who forfeited can no longer submit into the match.
        if (participant.isForfeit()) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_125, List.of(userId, matchId));
        }
    }
}
