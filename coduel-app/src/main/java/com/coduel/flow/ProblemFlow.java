package com.coduel.flow;

import com.coduel.api.ProblemApi;
import com.coduel.api.TestCaseApi;
import com.coduel.common.data.PageData;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Problem;
import com.coduel.entity.TestCase;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.result.ProblemWithVisibleTestCases;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(rollbackFor = ApiException.class)
public class ProblemFlow {

    @Autowired
    private ProblemApi problemApi;
    @Autowired
    private TestCaseApi testCaseApi;

    public ProblemWithVisibleTestCases create(Problem problem, List<TestCase> testCases) throws ApiException {
        Problem saved = problemApi.add(problem);
        for (TestCase testCase : testCases) {
            testCase.setProblemId(saved.getId());
            testCaseApi.add(testCase);
        }
        List<TestCase> visibleTestCases = testCases.stream().filter(testCase -> !testCase.isHidden()).toList();
        return ConversionHelper.toResult(saved, visibleTestCases);
    }

    public ProblemWithVisibleTestCases getWithVisibleTestCases(String slug) throws ApiException {
        Problem problem = problemApi.getCheckBySlug(slug);
        List<TestCase> visibleTestCases = testCaseApi.getVisibleTestCases(problem.getId());
        return ConversionHelper.toResult(problem, visibleTestCases);
    }

    public PageData<ProblemWithVisibleTestCases> getPage(int page, int size) throws ApiException {
        int safePage = Math.max(0, page);

        List<Problem> problems = problemApi.getPage(safePage, size);
        List<Long> problemIds = problems.stream().map(Problem::getId).toList();
        List<TestCase> visibleTestCases = testCaseApi.getVisibleTestCasesByProblemIds(problemIds);
        List<ProblemWithVisibleTestCases> content = ConversionHelper.pairWithVisibleTestCases(problems, visibleTestCases);

        return ConversionHelper.toPage(content, safePage, size, problemApi.count());
    }
}
