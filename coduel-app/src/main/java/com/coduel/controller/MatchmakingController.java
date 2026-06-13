package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.MatchmakingDto;
import com.coduel.model.data.MatchmakingData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/matchmaking")
public class MatchmakingController {

    @Autowired
    private MatchmakingDto matchmakingDto;

    @PostMapping("/join")
    public MatchmakingData join(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return matchmakingDto.join(principal.getSubject());
    }

    @GetMapping("/status")
    public MatchmakingData status(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return matchmakingDto.status(principal.getSubject());
    }
}
