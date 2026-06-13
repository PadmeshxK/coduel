package com.coduel.model.result;

import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Everything the judge needs to evaluate a submission, loaded together by JudgeFlow. */
@Getter
@Setter
public class JudgingInputs {

    private Submission submission;
    private Problem problem;
    private List<TestCase> testCases;
}
