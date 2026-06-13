package com.coduel.flow;

import com.coduel.api.ProblemApi;
import com.coduel.api.SubmissionApi;
import com.coduel.api.TestCaseApi;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.result.JudgingInputs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(rollbackFor = ApiException.class)
public class JudgeFlow {

    @Autowired
    private SubmissionApi submissionApi;
    @Autowired
    private ProblemApi problemApi;
    @Autowired
    private TestCaseApi testCaseApi;

    // Orchestrates the three Apis needed to judge a submission: the submission, its problem,
    // and all of that problem's test cases (visible + hidden).
    public JudgingInputs loadInputs(Long submissionId) throws ApiException {
        Submission submission = submissionApi.getCheckById(submissionId);
        Problem problem = problemApi.getCheckById(submission.getProblemId());
        List<TestCase> testCases = testCaseApi.getTestCases(problem.getId());
        return ConversionHelper.toJudgingInputs(submission, problem, testCases);
    }
}
