package com.coduel.execution.helper;

import com.coduel.execution.model.config.LanguageConfig;
import com.coduel.execution.model.constant.Language;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConversionHelper {

    public static Map<Language, LanguageConfig> constructConfig() {
        Map<Language, LanguageConfig> configMap = new HashMap<>();
        configMap.put(Language.PYTHON, getConfig(Language.PYTHON));
        configMap.put(Language.CPP, getConfig(Language.CPP));
        return configMap;
    }

    private static LanguageConfig getConfig(Language language){
        LanguageConfig languageConfig = new LanguageConfig();
        switch (language){
            case PYTHON:
                languageConfig.setFileName("main.py");
                languageConfig.setRunCommand(List.of("python3", "main.py"));
                break;
            case CPP:
                languageConfig.setFileName("main.cpp");
                languageConfig.setCompileCommand(List.of(
                        "g++",
                        "-O2",
                        "-std=c++20",
                        "-Wall",
                        "main.cpp",
                        "-o",
                        "main"
                ));
                languageConfig.setRunCommand(List.of("./main"));
                break;
        }
        return languageConfig;
    }
}
