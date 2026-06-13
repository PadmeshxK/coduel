package com.coduel.model.form;

import com.coduel.common.annotation.NoTrim;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCaseForm {

    // @NoTrim: execution payload — stored byte-for-byte, never mutated.
    @NoTrim
    @NotNull(message = "input is required")
    private String input;

    @NoTrim
    @NotNull(message = "expectedOutput is required")
    private String expectedOutput;

    private boolean hidden;
}
