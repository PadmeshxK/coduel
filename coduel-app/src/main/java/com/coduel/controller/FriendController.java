package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.FriendDto;
import com.coduel.model.data.FriendData;
import com.coduel.model.data.FriendRequestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/friend")
public class FriendController {

    @Autowired
    private FriendDto friendDto;

    @GetMapping
    public List<FriendData> friends(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return friendDto.listFriends(principal.getSubject());
    }

    @GetMapping("/request")
    public List<FriendRequestData> requests(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return friendDto.listIncoming(principal.getSubject());
    }

    @PostMapping("/request")
    public void sendRequest(@RequestParam("userId") Long userId,
                            @AuthenticationPrincipal OidcUser principal) throws ApiException {
        friendDto.sendRequest(principal.getSubject(), userId);
    }

    @PostMapping("/request/{id}/accept")
    public void accept(@PathVariable("id") Long id,
                       @AuthenticationPrincipal OidcUser principal) throws ApiException {
        friendDto.accept(principal.getSubject(), id);
    }

    // Decline an incoming request, or cancel one you sent.
    @DeleteMapping("/request/{id}")
    public void decline(@PathVariable("id") Long id,
                        @AuthenticationPrincipal OidcUser principal) throws ApiException {
        friendDto.remove(principal.getSubject(), id);
    }

    @DeleteMapping("/{userId}")
    public void unfriend(@PathVariable("userId") Long userId,
                         @AuthenticationPrincipal OidcUser principal) throws ApiException {
        friendDto.unfriend(principal.getSubject(), userId);
    }
}
