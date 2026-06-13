package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.CodeExecutionDto;
import com.coduel.model.data.ExecutionData;
import com.coduel.model.form.ExecutionForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/code")
public class CodeExecutionController {

    @Autowired
    private CodeExecutionDto codeExecutionDto;

    @PostMapping("/execute")
    public ExecutionData executeCode(@RequestBody ExecutionForm form) throws ApiException {
        return codeExecutionDto.executeCode(form);
    }
}
