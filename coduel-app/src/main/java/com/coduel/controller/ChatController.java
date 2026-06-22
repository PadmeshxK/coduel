package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.ChatDto;
import com.coduel.model.data.ConversationData;
import com.coduel.model.data.MessageData;
import com.coduel.model.form.MessageForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatDto chatDto;

    // Send a DM to a friend → returns the persisted message (also pushed live to the recipient).
    @PostMapping("/messages")
    public MessageData send(@RequestBody MessageForm form,
                            @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.sendMessage(principal.getSubject(), form);
    }

    // The DM inbox — my conversations, most-recent-first.
    @GetMapping("/conversations")
    public List<ConversationData> conversations(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.listConversations(principal.getSubject());
    }

    // A thread page — newest-first; pass ?before={messageId} to load older history (keyset).
    @GetMapping("/conversations/{id}/messages")
    public List<MessageData> messages(@PathVariable("id") Long conversationId,
                                      @RequestParam(value = "before", required = false) Long before,
                                      @RequestParam(value = "size", defaultValue = "30") int size,
                                      @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.loadMessages(principal.getSubject(), conversationId, before, size);
    }

    // Mark a thread read up to now (persists the caller's read marker) — clears its unread badge.
    @PostMapping("/conversations/{id}/read")
    public void read(@PathVariable("id") Long conversationId,
                     @AuthenticationPrincipal OidcUser principal) throws ApiException {
        chatDto.markRead(principal.getSubject(), conversationId);
    }
}
