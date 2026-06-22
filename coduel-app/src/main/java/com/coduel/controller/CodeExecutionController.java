package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.CodeExecutionDto;
import com.coduel.model.data.RunAcceptedData;
import com.coduel.model.form.ExecutionForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/code")
public class CodeExecutionController {

    @Autowired
    private CodeExecutionDto codeExecutionDto;

    // Queue the run (async) and return 202 + the runId; the result arrives over /user/queue/run-result.
    @PostMapping("/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunAcceptedData executeCode(@RequestBody ExecutionForm form,
                                       @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return codeExecutionDto.executeCode(form, principal.getSubject());
    }
}
