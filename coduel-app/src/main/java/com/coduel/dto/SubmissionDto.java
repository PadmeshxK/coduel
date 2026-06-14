package com.coduel.dto;

import com.coduel.api.SubmissionApi;
import com.coduel.api.UserApi;
import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Submission;
import com.coduel.flow.SubmissionFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.SubmissionData;
import com.coduel.model.form.SubmissionForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SubmissionDto extends AbstractDto {

    @Autowired
    private SubmissionFlow submissionFlow;
    @Autowired
    private SubmissionApi submissionApi;
    @Autowired
    private UserApi userApi;

    @Transactional(rollbackFor = ApiException.class)
    public SubmissionData create(SubmissionForm form, String googleId) throws ApiException {
        checkValid(form);
        trim(form);
        Submission submission = ConversionHelper.convert(form);
        // Identity from the session, not the form: resolve the app userId from the principal's googleId.
        submission.setUserId(userApi.getCheckByGoogleId(googleId).getId());
        // Persist with dispatched=false (entity default). No Kafka here: the SubmissionRelay turns it into
        // a judge request after commit. Persisting the row AND "needs judging" is thus one atomic write —
        // no dual-write race (the judge only ever reads committed rows) and no orphan PENDING.
        Submission saved = submissionFlow.create(submission);
        return ConversionHelper.convert(saved);
    }

    public SubmissionData get(Long id) throws ApiException {
        return ConversionHelper.convert(submissionApi.getCheckById(id));
    }
}
