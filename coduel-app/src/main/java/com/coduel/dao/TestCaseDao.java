package com.coduel.dao;

import com.coduel.entity.TestCase;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TestCaseDao extends BaseDao<TestCase> {

    private static final String SELECT_VISIBLE_BY_PROBLEM =
            "SELECT t FROM TestCase t WHERE t.problemId = :problemId AND t.hidden = false ORDER BY t.id";
    private static final String SELECT_VISIBLE_BY_PROBLEMS =
            "SELECT t FROM TestCase t WHERE t.problemId IN :problemIds AND t.hidden = false ORDER BY t.id";
    private static final String SELECT_BY_PROBLEM =
            "SELECT t FROM TestCase t WHERE t.problemId = :problemId ORDER BY t.id";

    public TestCaseDao() {
        super(TestCase.class);
    }

    // ALL test cases (visible + hidden) — used by the judge worker; hidden cases are the real test.
    public List<TestCase> selectByProblemId(Long problemId) {
        return createQuery(SELECT_BY_PROBLEM, TestCase.class)
                .setParameter("problemId", problemId)
                .getResultList();
    }

    public List<TestCase> selectVisibleByProblemId(Long problemId) {
        return createQuery(SELECT_VISIBLE_BY_PROBLEM, TestCase.class)
                .setParameter("problemId", problemId)
                .getResultList();
    }

    public List<TestCase> selectVisibleByProblemIds(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return List.of();
        }
        return createQuery(SELECT_VISIBLE_BY_PROBLEMS, TestCase.class)
                .setParameter("problemIds", problemIds)
                .getResultList();
    }
}
