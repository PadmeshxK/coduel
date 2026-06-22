package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.UserDto;
import com.coduel.model.data.FriendData;
import com.coduel.model.data.UserProfileData;
import com.coduel.model.form.UserProfileForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
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

    // Public directory: find people to add as friends, by display-name prefix. Each hit carries the
    // caller's relationship to it (friend / pending) so the UI shows the right action.
    @GetMapping("/search")
    public List<FriendData> search(@RequestParam("q") String query,
                                   @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return userDto.search(query, principal.getSubject());
    }

    // Live availability check for the name-setup / profile-edit UI.
    @GetMapping("/display-name-available")
    public Map<String, Boolean> displayNameAvailable(@RequestParam("name") String name,
                                                     @AuthenticationPrincipal OidcUser principal)
            throws ApiException {
        return Map.of("available", userDto.isDisplayNameAvailable(name, principal.getSubject()));
    }
}
