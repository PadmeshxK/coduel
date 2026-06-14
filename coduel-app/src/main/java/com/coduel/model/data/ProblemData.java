package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProblemData {

    private Long id;
    private String slug;
    private String title;
    private String statement;
    private Integer timeLimitMs;
    private List<TestCaseData> testCases;
}
