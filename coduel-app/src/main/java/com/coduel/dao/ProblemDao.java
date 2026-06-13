package com.coduel.dao;

import com.coduel.entity.Problem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProblemDao extends BaseDao<Problem> {

    private static final String SELECT_BY_SLUG = "SELECT p FROM Problem p WHERE p.slug = :slug";

    public ProblemDao() {
        super(Problem.class);
    }

    public Problem selectBySlug(String slug) {
        return selectSingleOrNull(createQuery(SELECT_BY_SLUG, Problem.class).setParameter("slug", slug));
    }

    // ORDER BY RAND() is fine for a small catalog; revisit if the problem set grows large.
    @SuppressWarnings("unchecked")
    public Problem selectRandom() {
        List<Problem> result = em()
                .createNativeQuery("SELECT * FROM problem ORDER BY RAND() LIMIT 1", Problem.class)
                .getResultList();
        return result.isEmpty() ? null : result.get(0);
    }
}
