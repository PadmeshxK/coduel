package com.coduel.model.data;

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
    private Long winnerUserId;
}
