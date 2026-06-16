package com.coduel.dto;

import com.coduel.common.data.PageData;
import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Problem;
import com.coduel.entity.TestCase;
import com.coduel.flow.ProblemFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.ProblemData;
import com.coduel.model.form.ProblemForm;
import com.coduel.model.result.ProblemWithVisibleTestCases;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProblemDto extends AbstractDto {

    @Autowired
    private ProblemFlow problemFlow;

    public ProblemData create(ProblemForm form) throws ApiException {
        checkValid(form);
        trim(form);
        Problem problem = ConversionHelper.convert(form);
        List<TestCase> testCases = ConversionHelper.toTestCases(form);
        return ConversionHelper.convert(problemFlow.create(problem, testCases));
    }

    // Validate + convert the whole batch up front, then persist it in one transaction (the Flow).
    public List<ProblemData> createBatch(List<ProblemForm> forms) throws ApiException {
        checkValid(forms);
        trim(forms);
        List<Problem> problems = ConversionHelper.toProblems(forms);
        List<List<TestCase>> testCasesPerProblem = ConversionHelper.toTestCaseGroups(forms);
        return ConversionHelper.toProblemDataList(problemFlow.createBatch(problems, testCasesPerProblem));
    }

    public ProblemData getBySlug(String slug) throws ApiException {
        return ConversionHelper.convert(problemFlow.getWithVisibleTestCases(slug));
    }

    public PageData<ProblemData> getPage(int page, int size) throws ApiException {
        PageData<ProblemWithVisibleTestCases> result = problemFlow.getPage(page, size);
        List<ProblemData> content = result.getContent().stream().map(ConversionHelper::convert).toList();
        return ConversionHelper.toPage(content, result.getPage(), result.getSize(), result.getTotalElements());
    }
}
