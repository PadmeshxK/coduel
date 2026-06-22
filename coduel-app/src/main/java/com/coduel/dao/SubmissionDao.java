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
    // A user's full history for one problem (solo + duel), latest first.
    private static final String SELECT_BY_USER_AND_PROBLEM =
            "SELECT s FROM Submission s WHERE s.userId = :userId AND s.problemId = :problemId ORDER BY s.createdAt DESC, s.id DESC";
    // (problemId, verdict) for all of a user's submissions, newest first — reduced to latest-per-problem.
    private static final String SELECT_PROBLEM_STATUSES =
            "SELECT s.problemId, s.verdict FROM Submission s WHERE s.userId = :userId ORDER BY s.createdAt DESC";

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

    public List<Submission> selectByUserIdAndProblemId(Long userId, Long problemId) {
        return createQuery(SELECT_BY_USER_AND_PROBLEM, Submission.class)
                .setParameter("userId", userId)
                .setParameter("problemId", problemId)
                .setMaxResults(20) // latest 20 — bounds the payload (each row carries source code)
                .getResultList();
    }

    public List<Object[]> selectProblemStatuses(Long userId) {
        return createQuery(SELECT_PROBLEM_STATUSES, Object[].class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
