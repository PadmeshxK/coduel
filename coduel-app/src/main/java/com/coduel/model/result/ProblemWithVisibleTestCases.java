package com.coduel.model.result;

import com.coduel.entity.Problem;
import com.coduel.entity.TestCase;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Internal carrier (not serialized): a problem plus its visible (hidden=false) test cases. */
@Getter
@Setter
public class ProblemWithVisibleTestCases {

    private Problem problem;
    private List<TestCase> visibleTestCases;
}
