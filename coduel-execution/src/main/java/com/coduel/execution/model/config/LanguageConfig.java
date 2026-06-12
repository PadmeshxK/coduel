package com.coduel.execution.model.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LanguageConfig {

    private String fileName;
    private List<String> compileCommand;
    private List<String> runCommand;

}
