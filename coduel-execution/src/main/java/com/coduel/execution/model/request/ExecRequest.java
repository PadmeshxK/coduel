package com.coduel.execution.model.request;

import com.coduel.execution.model.constant.Language;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class ExecRequest {

    private Language language;
    private String code;
    private String stdin;
    private Duration timeout;

}
