package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.SubmissionDao;
import com.coduel.entity.Submission;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.Verdict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class SubmissionApi extends AbstractApi {

    @Autowired
    private SubmissionDao submissionDao;

    public Submission add(Submission submission) {
        return submissionDao.persist(submission);
    }

    public Submission getCheckById(Long id) throws ApiException {
        Submission submission = submissionDao.selectById(id);
        if (Objects.isNull(submission)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_106, List.of(id));
        }
        return submission;
    }

    // Loads the managed entity and mutates it; JPA dirty-checking flushes the UPDATE on commit.
    public void updateVerdict(Long id, Verdict verdict, Long runtimeMs, Integer passedTests, Integer totalTests)
            throws ApiException {
        Submission submission = getCheckById(id);
        submission.setVerdict(verdict);
        submission.setRuntimeMs(runtimeMs);
        submission.setPassedTests(passedTests);
        submission.setTotalTests(totalTests);
    }

    // Outbox: submissions persisted but not yet relayed to the judge queue.
    public List<Submission> getUndispatched(int limit) {
        return submissionDao.selectUndispatched(limit);
    }

    public void markDispatched(Long id) throws ApiException {
        getCheckById(id).setDispatched(true);
    }
}
