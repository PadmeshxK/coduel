package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.TestCaseDao;
import com.coduel.entity.TestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = ApiException.class)
public class TestCaseApi extends AbstractApi {

    @Autowired
    private TestCaseDao testCaseDao;

    public TestCase add(TestCase testCase) {
        return testCaseDao.persist(testCase);
    }

    public List<TestCase> getVisibleTestCases(Long problemId) {
        return testCaseDao.selectVisibleByProblemId(problemId);
    }

    public List<TestCase> getVisibleTestCasesByProblemIds(List<Long> problemIds) {
        return testCaseDao.selectVisibleByProblemIds(problemIds);
    }

    // ALL test cases (visible + hidden) for judging.
    public List<TestCase> getTestCases(Long problemId) {
        return testCaseDao.selectByProblemId(problemId);
    }
}
