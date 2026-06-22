package com.coduel.model.result;

import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import com.coduel.model.constant.Verdict;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Internal carrier: a problem, its visible (hidden=false) test cases, and (optionally) the
 *  requesting user's view — latest verdict (list) or full submission history (detail). */
@Getter
@Setter
public class VisibleProblemResult {

    private Problem problem;
    private List<TestCase> visibleTestCases;
    private Verdict status;
    // True if the user has ever solved this problem (any ACCEPTED submission) — survives later misses.
    private boolean solved;
    private List<Submission> submissions;
}
