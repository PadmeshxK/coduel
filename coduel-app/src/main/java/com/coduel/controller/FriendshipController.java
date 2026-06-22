package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.FriendshipDto;
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
public class FriendshipController {

    @Autowired
    private FriendshipDto friendshipDto;

    @GetMapping
    public List<FriendData> friends(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return friendshipDto.listFriends(principal.getSubject());
    }

    @GetMapping("/request")
    public List<FriendRequestData> requests(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return friendshipDto.listIncoming(principal.getSubject());
    }

    @PostMapping("/request")
    public void sendRequest(@RequestParam("userId") Long userId,
                            @AuthenticationPrincipal OidcUser principal) throws ApiException {
        friendshipDto.sendRequest(principal.getSubject(), userId);
    }

    @PostMapping("/request/{id}/accept")
    public void accept(@PathVariable("id") Long id,
                       @AuthenticationPrincipal OidcUser principal) throws ApiException {
        friendshipDto.accept(principal.getSubject(), id);
    }

    // Decline an incoming request, or cancel one you sent.
    @DeleteMapping("/request/{id}")
    public void decline(@PathVariable("id") Long id,
                        @AuthenticationPrincipal OidcUser principal) throws ApiException {
        friendshipDto.remove(principal.getSubject(), id);
    }

    @DeleteMapping("/{userId}")
    public void unfriend(@PathVariable("userId") Long userId,
                         @AuthenticationPrincipal OidcUser principal) throws ApiException {
        friendshipDto.unfriend(principal.getSubject(), userId);
    }
}
