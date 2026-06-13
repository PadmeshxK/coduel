package com.coduel.model.form;

import com.coduel.common.annotation.NoTrim;
import com.coduel.execution.model.constant.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionForm {

    @NotNull(message = "problemId is required")
    private Long problemId;

    // optional — null = solo submission (no duel).
    private Long matchId;

    @NotNull(message = "language is required")
    private Language language;

    // @NoTrim: source code is execution payload — leading/trailing whitespace is significant.
    @NoTrim
    @NotBlank(message = "sourceCode must not be blank")
    private String sourceCode;
}
