package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.ChallengeDto;
import com.coduel.model.data.ChallengeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/challenge")
public class ChallengeController {

    @Autowired
    private ChallengeDto challengeDto;

    // Challenge a friend to a duel → returns the challengeId so the challenger can show "waiting…".
    @PostMapping
    public ChallengeData create(@RequestParam("userId") Long userId,
                                @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return challengeDto.create(principal.getSubject(), userId);
    }

    // Accept a challenge sent to me → returns the matchId to jump into.
    @PostMapping("/{id}/accept")
    public ChallengeData accept(@PathVariable("id") String id,
                                @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return challengeDto.accept(id, principal.getSubject());
    }

    @PostMapping("/{id}/decline")
    public void decline(@PathVariable("id") String id,
                        @AuthenticationPrincipal OidcUser principal) throws ApiException {
        challengeDto.decline(id, principal.getSubject());
    }
}
