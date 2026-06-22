package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.ProblemDao;
import com.coduel.entity.Problem;
import com.coduel.model.constant.Errors;
import com.coduel.model.form.ProblemFilterForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class ProblemApi extends AbstractApi {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private ProblemDao problemDao;

    public Problem add(Problem problem) throws ApiException {
        if (!Objects.isNull(problemDao.selectBySlug(problem.getSlug()))) {
            throw new ApiException(ApiStatus.RESOURCE_EXISTS, Errors.ERR_104, List.of(problem.getSlug()));
        }
        return problemDao.persist(problem);
    }

    // Combinable filters (search / ratings / tags / status) + sort + pagination. userId (nullable)
    // backs the solved/unsolved bits; it comes from the session, never the filter form.
    public List<Problem> getPage(ProblemFilterForm filter, Long userId, int page, int size) throws ApiException {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_105, List.of(MAX_PAGE_SIZE, size));
        }
        return problemDao.selectPageFiltered(filter, userId, page, size);
    }

    public long count(ProblemFilterForm filter, Long userId) {
        return problemDao.countFiltered(filter, userId);
    }

    public List<String> getFilteredSlugs(ProblemFilterForm filter, Long userId) {
        return problemDao.selectFilteredSlugs(filter, userId);
    }

    public List<Integer> getDistinctRatings() {
        return problemDao.selectDistinctRatings();
    }

    public List<String> getDistinctTags() {
        return problemDao.selectDistinctTags();
    }

    public Problem getCheckById(Long id) throws ApiException {
        Problem problem = problemDao.selectById(id);
        if (Objects.isNull(problem)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_107, List.of(id));
        }
        return problem;
    }

    public Problem getCheckBySlug(String slug) throws ApiException {
        checkNotNull(slug, com.coduel.common.constant.Errors.ERR_001, List.of("slug"));
        Problem problem = problemDao.selectBySlug(slug);
        if (Objects.isNull(problem)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_103, List.of(slug));
        }
        return problem;
    }

    public Problem getCheckRandomProblem() throws ApiException {
        Problem problem = problemDao.selectRandom();
        if (Objects.isNull(problem)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_111, List.of());
        }
        return problem;
    }
}
