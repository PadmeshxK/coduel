package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.UserDto;
import com.coduel.model.data.UserProfileData;
import com.coduel.model.form.UserProfileForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class UserController {

    @Autowired
    private UserDto userDto;

    // Current user's stored profile. Also serves as the SPA's auth check (401 if no session).
    @GetMapping("/profile")
    public UserProfileData getProfile(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return userDto.getProfile(principal.getSubject());
    }

    @PatchMapping("/profile")
    public UserProfileData updateProfile(@RequestBody UserProfileForm form,
                                         @AuthenticationPrincipal OidcUser principal) throws ApiException {
        // googleId from the session principal, never the body.
        return userDto.updateProfile(form, principal.getSubject());
    }
}
