package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.PresenceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/presence")
public class PresenceController {

    @Autowired
    private PresenceDto presenceDto;

    // The userIds of my friends who are currently online — live updates then arrive on /user/queue/presence.
    @GetMapping("/friends")
    public List<Long> onlineFriends(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return presenceDto.getOnlineFriendIds(principal.getSubject());
    }
}
