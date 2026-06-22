package com.coduel.dto;

import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Submission;
import com.coduel.flow.SubmissionFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.SubmissionData;
import com.coduel.model.form.SubmissionForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubmissionDto extends AbstractDto {

    @Autowired
    private SubmissionFlow submissionFlow;

    public SubmissionData create(SubmissionForm form, String googleId) throws ApiException {
        checkValid(form);
        trim(form);
        Submission submission = ConversionHelper.convert(form);
        return ConversionHelper.convert(submissionFlow.create(submission, googleId));
    }

    public SubmissionData get(Long id, String googleId) throws ApiException {
        return ConversionHelper.convert(submissionFlow.getOwned(id, googleId));
    }
}
