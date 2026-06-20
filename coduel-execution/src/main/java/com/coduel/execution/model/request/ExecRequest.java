package com.coduel.execution.model.request;

import com.coduel.execution.model.constant.Language;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
public class ExecRequest {

    private Language language;
    private String code;
    private List<TestCase> testCases;
    private Duration timeout;

}
