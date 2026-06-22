package com.coduel.dao;

import com.coduel.entity.Problem;
import com.coduel.model.constant.ProblemStatusFilter;
import com.coduel.model.constant.Verdict;
import com.coduel.model.form.ProblemFilterForm;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProblemDao extends BaseDao<Problem> {

    private static final String SELECT_BY_SLUG = "SELECT p FROM Problem p WHERE p.slug = :slug";
    // The user has an accepted submission for this problem (drives solved/unsolved filter + sort).
    private static final String ACCEPTED_EXISTS =
            "EXISTS (SELECT 1 FROM Submission s WHERE s.userId = :userId AND s.problemId = p.id "
                    + "AND s.verdict = :accepted)";
    // The problem carries at least one of the requested tags. Subquery (not JOIN) so no row dup / DISTINCT.
    private static final String TAG_MATCH =
            "EXISTS (SELECT 1 FROM Problem pt JOIN pt.tags t WHERE pt.id = p.id AND t IN :tags)";
    private static final String SOLVED_LAST = "CASE WHEN " + ACCEPTED_EXISTS + " THEN 1 ELSE 0 END ASC, ";

    public ProblemDao() {
        super(Problem.class);
    }

    public Problem selectBySlug(String slug) {
        return selectSingleOrNull(createQuery(SELECT_BY_SLUG, Problem.class).setParameter("slug", slug));
    }

    // ORDER BY RAND() is fine for a small catalog; revisit if the problem set grows large.
    public Problem selectRandom() {
        List<Problem> result = em()
                .createNativeQuery("SELECT * FROM problem ORDER BY RAND() LIMIT 1", Problem.class)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    public List<Problem> selectPageFiltered(ProblemFilterForm f, Long userId, int page, int size) {
        boolean unsolvedFirst = "unsolved".equals(f.getSort()) && userId != null;
        String jpql = "SELECT p FROM Problem p" + where(f, userId)
                + " ORDER BY " + (unsolvedFirst ? SOLVED_LAST : "") + ratingOrder(f.getSort()) + ", p.id ASC";
        TypedQuery<Problem> query = createQuery(jpql, Problem.class);
        bind(query, f, userId, unsolvedFirst);
        return query.setFirstResult(page * size).setMaxResults(size).getResultList();
    }

    public long countFiltered(ProblemFilterForm f, Long userId) {
        TypedQuery<Long> query = createQuery("SELECT COUNT(p) FROM Problem p" + where(f, userId), Long.class);
        bind(query, f, userId, false);
        return query.getSingleResult();
    }

    // All matching slugs in the same order as the list — lets the Solve page find the "next" problem
    // within the active filter. Slug-only projection keeps it cheap even for the whole catalog.
    public List<String> selectFilteredSlugs(ProblemFilterForm f, Long userId) {
        boolean unsolvedFirst = "unsolved".equals(f.getSort()) && userId != null;
        String jpql = "SELECT p.slug FROM Problem p" + where(f, userId)
                + " ORDER BY " + (unsolvedFirst ? SOLVED_LAST : "") + ratingOrder(f.getSort()) + ", p.id ASC";
        TypedQuery<String> query = createQuery(jpql, String.class);
        bind(query, f, userId, unsolvedFirst);
        return query.getResultList();
    }

    // Distinct values present in the catalog — for building the filter controls.
    public List<Integer> selectDistinctRatings() {
        return createQuery(
                "SELECT DISTINCT p.rating FROM Problem p WHERE p.rating IS NOT NULL ORDER BY p.rating",
                Integer.class).getResultList();
    }

    public List<String> selectDistinctTags() {
        return createQuery("SELECT DISTINCT t FROM Problem p JOIN p.tags t ORDER BY t", String.class)
                .getResultList();
    }

    // ---- dynamic query assembly ----
    private String where(ProblemFilterForm f, Long userId) {
        List<String> conds = new ArrayList<>();
        if (hasQ(f)) {
            conds.add("LOWER(p.title) LIKE :q");
        }
        if (hasRatings(f)) {
            conds.add("p.rating IN :ratings");
        }
        if (hasTags(f)) {
            conds.add(TAG_MATCH);
        }
        if (statusFilter(f, userId)) {
            conds.add((f.getStatus() == ProblemStatusFilter.SOLVED ? "" : "NOT ") + ACCEPTED_EXISTS);
        }
        return conds.isEmpty() ? "" : " WHERE " + String.join(" AND ", conds);
    }

    private void bind(TypedQuery<?> query, ProblemFilterForm f, Long userId, boolean unsolvedFirst) {
        if (hasQ(f)) {
            query.setParameter("q", "%" + f.getQ().toLowerCase() + "%");
        }
        if (hasRatings(f)) {
            query.setParameter("ratings", f.getRatings());
        }
        if (hasTags(f)) {
            query.setParameter("tags", f.getTags());
        }
        // userId/accepted are referenced by the status filter and/or the unsolved-first ordering.
        if (unsolvedFirst || statusFilter(f, userId)) {
            query.setParameter("userId", userId);
            query.setParameter("accepted", Verdict.ACCEPTED);
        }
    }

    private boolean hasQ(ProblemFilterForm f) {
        return f.getQ() != null && !f.getQ().isBlank();
    }

    private boolean hasRatings(ProblemFilterForm f) {
        return f.getRatings() != null && !f.getRatings().isEmpty();
    }

    private boolean hasTags(ProblemFilterForm f) {
        return f.getTags() != null && !f.getTags().isEmpty();
    }

    // A solved/unsolved filter only applies when we know the user (ALL/null = no filter).
    private boolean statusFilter(ProblemFilterForm f, Long userId) {
        return userId != null
                && (f.getStatus() == ProblemStatusFilter.SOLVED || f.getStatus() == ProblemStatusFilter.UNSOLVED);
    }

    // Un-rated problems (null rating) always sort to the end, both directions.
    private String ratingOrder(String sort) {
        return "rating-desc".equals(sort) ? "p.rating DESC NULLS LAST" : "p.rating ASC NULLS LAST";
    }
}
