package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.MatchDto;
import com.coduel.model.data.MatchData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/match")
public class MatchController {

    @Autowired
    private MatchDto matchDto;

    @GetMapping("/{id}")
    public MatchData get(@PathVariable("id") Long id,
                         @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return matchDto.getMatch(id, principal.getSubject());
    }

    // Give up an active match — the opponent wins.
    @PostMapping("/{id}/forfeit")
    public void forfeit(@PathVariable("id") Long id,
                        @AuthenticationPrincipal OidcUser principal) throws ApiException {
        matchDto.forfeit(id, principal.getSubject());
    }
}
