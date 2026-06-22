package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.SubmissionDto;
import com.coduel.model.data.SubmissionData;
import com.coduel.model.form.SubmissionForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/submission")
public class SubmissionController {

    @Autowired
    private SubmissionDto submissionDto;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)   // 202: accepted for async judging, not yet judged
    public SubmissionData create(@RequestBody SubmissionForm form,
                                 @AuthenticationPrincipal OidcUser principal) throws ApiException {
        // userId comes from the authenticated principal (sub = googleId), never from the form.
        return submissionDto.create(form, principal.getSubject());
    }

    @GetMapping("/{id}")
    public SubmissionData get(@PathVariable("id") Long id,
                             @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return submissionDto.get(id, principal.getSubject());
    }
}
