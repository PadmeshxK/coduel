package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.ProblemDao;
import com.coduel.entity.Problem;
import com.coduel.model.constant.Errors;
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

    public List<Problem> getPage(int page, int size) throws ApiException {
        if(size < 1 || size > MAX_PAGE_SIZE){
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_105, List.of(MAX_PAGE_SIZE, size));
        }
        return problemDao.selectPage(page, size);
    }

    public long count() {
        return problemDao.count();
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

    public Problem getRandomCheck() throws ApiException {
        Problem problem = problemDao.selectRandom();
        if (Objects.isNull(problem)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_111, List.of());
        }
        return problem;
    }
}
