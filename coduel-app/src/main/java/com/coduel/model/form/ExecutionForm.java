package com.coduel.model.form;

import com.coduel.common.annotation.NoTrim;
import com.coduel.execution.model.constant.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ExecutionForm {

    @NotNull(message = "is required")
    private Language language;

    // @NoTrim: execution payload, run byte-for-byte as sent.
    @NoTrim
    @NotBlank(message = "must not be blank")
    @Size(max = 64_000, message = "exceeds the maximum allowed size")
    private String code;

    // Cases to run against. Run sends the visible (+ any custom) cases; a blank expectedOutput on a
    // case means "just run it" (no comparison). Same shape the judge uses for the full case set.
    @NotNull(message = "testCases is required")
    private List<TestCaseForm> testCases;

    private Long timeoutMs;
}
