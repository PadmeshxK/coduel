package com.coduel.flow;

import com.coduel.api.ProblemApi;
import com.coduel.api.SubmissionApi;
import com.coduel.api.TestCaseApi;
import com.coduel.api.UserApi;
import com.coduel.common.data.PageData;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Problem;
import com.coduel.entity.Submission;
import com.coduel.entity.TestCase;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Verdict;
import com.coduel.model.result.VisibleProblemResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Transactional(rollbackFor = ApiException.class)
public class ProblemFlow {

    @Autowired
    private ProblemApi problemApi;
    @Autowired
    private TestCaseApi testCaseApi;
    @Autowired
    private SubmissionApi submissionApi;
    @Autowired
    private UserApi userApi;

    public VisibleProblemResult create(Problem problem, List<TestCase> testCases) throws ApiException {
        Problem saved = problemApi.add(problem);
        for (TestCase testCase : testCases) {
            testCase.setProblemId(saved.getId());
            testCaseApi.add(testCase);
        }
        List<TestCase> visibleTestCases = testCases.stream().filter(testCase -> !testCase.isHidden()).toList();
        return ConversionHelper.toResult(saved, visibleTestCases);
    }

    // Persists every problem + its test cases in ONE transaction — if any (e.g. a duplicate slug)
    // fails, the whole batch rolls back. Self-call to create() joins this transaction.
    public List<VisibleProblemResult> createBatch(List<Problem> problems,
                                                         List<List<TestCase>> testCasesPerProblem) throws ApiException {
        List<VisibleProblemResult> results = new ArrayList<>();
        for (int i = 0; i < problems.size(); i++) {
            results.add(create(problems.get(i), testCasesPerProblem.get(i)));
        }
        return results;
    }

    public VisibleProblemResult getWithVisibleTestCases(String slug, String googleId) throws ApiException {
        Problem problem = problemApi.getCheckBySlug(slug);
        List<TestCase> visibleTestCases = testCaseApi.getVisibleTestCases(problem.getId());
        VisibleProblemResult result = ConversionHelper.toResult(problem, visibleTestCases);
        // Bundle the user's submissions so the Solve page needs no extra round-trip on load.
        if (googleId != null) {
            Long userId = userApi.getCheckByGoogleId(googleId).getId();
            result.setSubmissions(submissionApi.getByUserAndProblem(userId, problem.getId()));
        }
        return result;
    }

    public PageData<VisibleProblemResult> getPage(int page, int size, String googleId) throws ApiException {
        int safePage = Math.max(0, page);

        List<Problem> problems = problemApi.getPage(safePage, size);
        List<Long> problemIds = problems.stream().map(Problem::getId).toList();
        List<TestCase> visibleTestCases = testCaseApi.getVisibleTestCasesByProblemIds(problemIds);
        List<VisibleProblemResult> content = ConversionHelper.pairWithVisibleTestCases(problems, visibleTestCases);
        attachStatuses(content, googleId);

        return ConversionHelper.toPage(content, safePage, size, problemApi.count());
    }

    // Stamp each result with the signed-in user's latest verdict per problem (no-op when anonymous).
    private void attachStatuses(List<VisibleProblemResult> content, String googleId) throws ApiException {
        if (googleId == null) {
            return;
        }
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        Map<Long, Verdict> statuses = new HashMap<>();
        for (Object[] row : submissionApi.getProblemStatuses(userId)) {
            statuses.putIfAbsent((Long) row[0], (Verdict) row[1]); // rows newest-first → first = latest
        }
        content.forEach(result -> result.setStatus(statuses.get(result.getProblem().getId())));
    }
}
