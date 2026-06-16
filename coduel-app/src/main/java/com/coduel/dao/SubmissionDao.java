package com.coduel.dao;

import com.coduel.entity.Submission;
import com.coduel.model.constant.Verdict;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class SubmissionDao extends BaseDao<Submission> {

    // Unrelayed + still unjudged, oldest first. The verdict guard means pre-existing judged rows
    // (which get dispatched=false when the column is added) are never re-dispatched.
    private static final String SELECT_UNDISPATCHED =
            "SELECT s FROM Submission s WHERE s.dispatched = false AND s.verdict = :pending ORDER BY s.id";
    private static final String SELECT_BY_MATCH_ID =
            "SELECT s FROM Submission s WHERE s.matchId = :matchId ORDER BY s.id ASC";
    // Orphans: still PENDING past the cutoff — judging never completed (never delivered/processed).
    private static final String SELECT_PENDING_OLDER =
            "SELECT s FROM Submission s WHERE s.verdict = :pending AND s.createdAt < :cutoff ORDER BY s.id";

    public SubmissionDao() {
        super(Submission.class);
    }

    public List<Submission> selectUndispatched(int limit) {
        return createQuery(SELECT_UNDISPATCHED, Submission.class)
                .setParameter("pending", Verdict.PENDING)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Submission> selectByMatchId(Long matchId) {
        return createQuery(SELECT_BY_MATCH_ID, Submission.class).
                setParameter("matchId", matchId).
                getResultList();
    }

    public List<Submission> selectPendingOlderThan(Instant cutoff) {
        return createQuery(SELECT_PENDING_OLDER, Submission.class)
                .setParameter("pending", Verdict.PENDING)
                .setParameter("cutoff", cutoff)
                .getResultList();
    }
}
