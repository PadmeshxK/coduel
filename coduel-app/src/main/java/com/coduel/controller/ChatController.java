package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.ChatDto;
import com.coduel.model.data.ConversationData;
import com.coduel.model.data.ConversationSettingData;
import com.coduel.model.data.MessageData;
import com.coduel.model.data.PinnedMessageData;
import com.coduel.model.data.UploadData;
import com.coduel.model.form.ConversationSettingForm;
import com.coduel.model.form.MessageEditForm;
import com.coduel.model.form.MessageForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    // Upload a chat image (multipart) → its stored URL, which the client then sends as an IMAGE message.
    @PostMapping("/upload")
    public UploadData upload(@RequestParam("file") MultipartFile file,
                             @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.uploadImage(file); // principal required (authenticated) — the vault validates the file
    }

    // Upload a voice note (multipart) → its stored URL, which the client then sends as a VOICE message.
    @PostMapping("/upload-audio")
    public UploadData uploadAudio(@RequestParam("file") MultipartFile file,
                                  @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.uploadAudio(file);
    }

    // The DM inbox — my conversations, most-recent-first.
    @GetMapping("/conversations")
    public List<ConversationData> conversations(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.listConversations(principal.getSubject());
    }

    // Search messages for a query, newest-first, paginated like the problem list (?page=&size=). Pass
    // ?conversationId= to scope to a single thread (in-thread search); omit it to search all threads.
    @GetMapping("/search")
    public com.coduel.common.data.PageData<com.coduel.model.data.MessageSearchData> search(
            @RequestParam("q") String q,
            @RequestParam(value = "conversationId", required = false) Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.searchMessages(principal.getSubject(), q, conversationId, page, size);
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

    // Set/replace my reaction on a message (one per message). DELETE clears it. Pushed live to the peer.
    @PostMapping("/messages/{messageId}/reaction")
    public void react(@PathVariable("messageId") Long messageId,
                      @RequestParam("emoji") String emoji,
                      @AuthenticationPrincipal OidcUser principal) throws ApiException {
        chatDto.react(principal.getSubject(), messageId, emoji);
    }

    @DeleteMapping("/messages/{messageId}/reaction")
    public void unreact(@PathVariable("messageId") Long messageId,
                        @AuthenticationPrincipal OidcUser principal) throws ApiException {
        chatDto.unreact(principal.getSubject(), messageId);
    }

    // Edit / soft-delete one of my own messages (pushed live to the peer on /user/queue/dm-update).
    @PatchMapping("/messages/{messageId}")
    public void edit(@PathVariable("messageId") Long messageId,
                     @RequestBody MessageEditForm form,
                     @AuthenticationPrincipal OidcUser principal) throws ApiException {
        chatDto.editMessage(principal.getSubject(), messageId, form);
    }

    @DeleteMapping("/messages/{messageId}")
    public void deleteMessage(@PathVariable("messageId") Long messageId,
                              @AuthenticationPrincipal OidcUser principal) throws ApiException {
        chatDto.deleteMessage(principal.getSubject(), messageId);
    }

    // Shared pins for a conversation (newest-first) + pin/unpin (pushed live to the peer).
    @GetMapping("/conversations/{id}/pins")
    public List<PinnedMessageData> pins(@PathVariable("id") Long conversationId,
                                        @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.listPins(principal.getSubject(), conversationId);
    }

    @PostMapping("/messages/{messageId}/pin")
    public void pin(@PathVariable("messageId") Long messageId,
                    @AuthenticationPrincipal OidcUser principal) throws ApiException {
        chatDto.pin(principal.getSubject(), messageId);
    }

    @DeleteMapping("/messages/{messageId}/pin")
    public void unpin(@PathVariable("messageId") Long messageId,
                      @AuthenticationPrincipal OidcUser principal) throws ApiException {
        chatDto.unpin(principal.getSubject(), messageId);
    }

    // My personalization for the thread with {peerId} (defaults if I never customized it).
    @GetMapping("/settings/{peerId}")
    public ConversationSettingData settings(@PathVariable("peerId") Long peerId,
                                            @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.getSettings(principal.getSubject(), peerId);
    }

    // Full-replace my personalization for the thread with {peerId} → returns the saved settings.
    @PutMapping("/settings/{peerId}")
    public ConversationSettingData updateSettings(@PathVariable("peerId") Long peerId,
                                                  @RequestBody ConversationSettingForm form,
                                                  @AuthenticationPrincipal OidcUser principal) throws ApiException {
        return chatDto.updateSettings(principal.getSubject(), peerId, form);
    }
}
