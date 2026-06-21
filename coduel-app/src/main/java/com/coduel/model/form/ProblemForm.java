package com.coduel.model.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProblemForm {

    @NotBlank(message = "slug must not be blank")
    @Size(max = 100, message = "slug is too long")
    private String slug;

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotBlank(message = "statement must not be blank")
    private String statement;

    private Integer timeLimitMs;

    // Optional difficulty rating (Codeforces-style). Left null when the source doesn't provide one.
    private Integer rating;

    // Optional topic tags (e.g. "dp", "math"). Empty when the source doesn't provide them.
    private List<String> tags;

    @NotEmpty(message = "at least one test case is required")
    @Valid
    private List<TestCaseForm> testCases;
}
