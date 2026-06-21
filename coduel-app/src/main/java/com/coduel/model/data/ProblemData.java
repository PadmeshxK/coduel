package com.coduel.model.data;

import com.coduel.model.constant.Verdict;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProblemData {

    private Long id;
    private String slug;
    private String title;
    private String statement;
    private Integer timeLimitMs;
    private Integer rating;
    private List<String> tags;
    private List<TestCaseData> testCases;
    // The signed-in user's latest verdict for this problem (null = not attempted / anonymous request).
    // Populated on the list endpoint (getPage); the single-problem endpoint carries `submissions` instead.
    private Verdict status;
    // Permanent "solved" flag: true if the user has EVER had an ACCEPTED submission for this problem,
    // independent of `status` (a later wrong submission makes status WRONG_ANSWER but keeps solved=true).
    private boolean solved;
    // The signed-in user's submissions for this problem — populated by getBySlug only (null on the list).
    private List<SubmissionData> submissions;
}
