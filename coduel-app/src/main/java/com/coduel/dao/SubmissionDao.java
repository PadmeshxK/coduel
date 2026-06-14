package com.coduel.dao;

import com.coduel.entity.Submission;
import com.coduel.model.constant.Verdict;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SubmissionDao extends BaseDao<Submission> {

    // Unrelayed + still unjudged, oldest first. The verdict guard means pre-existing judged rows
    // (which get dispatched=false when the column is added) are never re-dispatched.
    private static final String SELECT_UNDISPATCHED =
            "SELECT s FROM Submission s WHERE s.dispatched = false AND s.verdict = :pending ORDER BY s.id";

    public SubmissionDao() {
        super(Submission.class);
    }

    public List<Submission> selectUndispatched(int limit) {
        return createQuery(SELECT_UNDISPATCHED, Submission.class)
                .setParameter("pending", Verdict.PENDING)
                .setMaxResults(limit)
                .getResultList();
    }
}
