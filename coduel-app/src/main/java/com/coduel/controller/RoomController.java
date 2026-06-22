package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.RoomDto;
import com.coduel.model.data.RoomChatData;
import com.coduel.model.data.RoomData;

import java.util.List;
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

@RestController
@RequestMapping("/room")
public class RoomController {

    @Autowired
    private RoomDto roomDto;

    // Open a new persistent room.
    @PostMapping
    public RoomData create(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return roomDto.create(principal.getSubject());
    }

    @GetMapping("/{id}")
    public RoomData get(@PathVariable("id") Long id,
                        @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return roomDto.get(id, principal.getSubject());
    }

    // Invite a friend to the room by userId.
    @PostMapping("/{id}/invite")
    public void invite(@PathVariable("id") Long id,
                       @RequestParam("userId") Long userId,
                       @AuthenticationPrincipal OidcUser principal) throws ApiException {
        roomDto.invite(id, userId, principal.getSubject());
    }

    // Accept an invite and join the room.
    @PostMapping("/{id}/join")
    public void join(@PathVariable("id") Long id,
                     @AuthenticationPrincipal OidcUser principal) throws ApiException {
        roomDto.join(id, principal.getSubject());
    }

    // Host starts a match for the current roster; returns the room with the new activeMatchId set.
    @PostMapping("/{id}/start")
    public RoomData start(@PathVariable("id") Long id,
                          @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return roomDto.start(principal.getSubject(), id);
    }

    // Mark yourself ready / not ready in the lobby (host readiness is implicit).
    @PostMapping("/{id}/ready")
    public RoomData ready(@PathVariable("id") Long id,
                          @RequestParam("ready") boolean ready,
                          @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return roomDto.setReady(principal.getSubject(), id, ready);
    }

    // Hydrate the lobby chat (recent messages, oldest-first) — members only.
    @GetMapping("/{id}/chat")
    public List<RoomChatData> chat(@PathVariable("id") Long id,
                                   @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return roomDto.getChat(principal.getSubject(), id);
    }

    // Leave the room. The host leaving as the last member closes it.
    @DeleteMapping("/{id}/leave")
    public void leave(@PathVariable("id") Long id,
                      @AuthenticationPrincipal OidcUser principal) throws ApiException {
        roomDto.leave(id, principal.getSubject());
    }
}
