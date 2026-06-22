package com.coduel.model.data;

import com.coduel.model.constant.MatchEndReason;
import com.coduel.model.constant.MatchEventType;
import com.coduel.model.constant.Verdict;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchEventData {

    private MatchEventType type;

    // SUBMISSION_JUDGED
    private Long submissionId;
    private Long userId;
    private Verdict verdict;
    private Integer passedTests;
    private Integer totalTests;

    // MATCH_OVER
    // winnerUserId is null when there is no winner (NO_SHOW_VOID, TIMEOUT). The client picks its
    // message from (endReason, whether it is the winner / loser / neither).
    private Long winnerUserId;
    private MatchEndReason endReason;
}
