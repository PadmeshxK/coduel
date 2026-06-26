package com.coduel.dto;

import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.api.MediaApi;
import com.coduel.flow.ChatFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.ChatReadPublisher;
import com.coduel.interfaces.DmPublisher;
import com.coduel.interfaces.MessageUpdatePublisher;
import com.coduel.interfaces.PinPublisher;
import com.coduel.interfaces.ReactionPublisher;
import com.coduel.interfaces.TypingPublisher;
import com.coduel.interfaces.UserNotificationPublisher;
import com.coduel.model.result.MarkReadResult;
import com.coduel.model.result.MessageUpdateResult;
import com.coduel.model.result.PinResult;
import com.coduel.model.result.ReactionResult;
import com.coduel.model.result.TypingSignalResult;
import com.coduel.model.data.ConversationData;
import com.coduel.model.data.ConversationSettingData;
import com.coduel.model.data.MessageData;
import com.coduel.model.data.PinnedMessageData;
import com.coduel.model.data.UploadData;
import com.coduel.model.form.ConversationSettingForm;
import com.coduel.model.form.MessageEditForm;
import com.coduel.model.form.MessageForm;
import com.coduel.model.result.DmSentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class ChatDto extends AbstractDto {

    @Autowired
    private ChatFlow chatFlow;
    @Autowired
    private DmPublisher dmPublisher;
    @Autowired
    private UserNotificationPublisher userNotificationPublisher;
    @Autowired
    private ChatReadPublisher chatReadPublisher;
    @Autowired
    private TypingPublisher typingPublisher;
    @Autowired
    private ReactionPublisher reactionPublisher;
    @Autowired
    private MessageUpdatePublisher messageUpdatePublisher;
    @Autowired
    private PinPublisher pinPublisher;
    @Autowired
    private MediaApi mediaApi;

    public MessageData sendMessage(String googleId, MessageForm form) throws ApiException {
        checkValid(form);
        trim(form);
        DmSentResult result = chatFlow.sendDirectMessage(
                googleId, form.getRecipientUserId(), form.getBody(), form.getReplyToId(),
                form.getKind(), form.getCodeLanguage(), form.getAttachmentUrl(), form.getSharedRef(),
                form.getDurationMs());
        MessageData data = ConversionHelper.toMessageData(result.getMessage(), List.of(), result.getReplyTo());
        // Two channels: the message itself for the live thread, and a notification so the recipient is
        // alerted even when they're not on the Messages page.
        dmPublisher.publish(result.getRecipientGoogleId(), data);
        // Muted = the recipient suppressed alerts from this sender. Still deliver the DM to the live
        // thread above; just skip the toast/bell. (The DM cue isn't a pending bell item, so there's
        // nothing left to reconcile.)
        if (!chatFlow.isMuted(form.getRecipientUserId(), result.getSender().getId())) {
            userNotificationPublisher.publish(result.getRecipientGoogleId(),
                    ConversionHelper.toDmNotification(result.getSender(), data.getKind()));
        }
        return data; // echoed back to the sender as the HTTP response
    }

    public com.coduel.common.data.PageData<com.coduel.model.data.MessageSearchData> searchMessages(
            String googleId, String query, Long conversationId, int page, int size) throws ApiException {
        return chatFlow.searchMessages(googleId, query, conversationId, page, size);
    }

    public List<ConversationData> listConversations(String googleId) throws ApiException {
        return chatFlow.listConversations(googleId).stream()
                .map(view -> ConversionHelper.toConversationData(
                        view.getConversation(), view.getOther(), view.isUnread(), view.getSetting()))
                .toList();
    }

    public void markRead(String googleId, Long conversationId) throws ApiException {
        MarkReadResult result = chatFlow.markRead(googleId, conversationId);
        // Tell the message author the thread was read up to now → drives their live "Seen" receipt.
        // null = the caller disabled read receipts for this peer, so we persist the marker but stay silent.
        if (result != null) {
            chatReadPublisher.publish(result.getOtherGoogleId(), result.getReceipt());
        }
    }

    public ConversationSettingData getSettings(String googleId, Long peerUserId) throws ApiException {
        return ConversionHelper.toConversationSettingData(chatFlow.getSettings(googleId, peerUserId));
    }

    public ConversationSettingData updateSettings(String googleId, Long peerUserId,
                                                  ConversationSettingForm form) throws ApiException {
        checkValid(form);
        trim(form);
        return ConversionHelper.toConversationSettingData(chatFlow.updateSettings(googleId, peerUserId, form));
    }

    // recipientUserIdRaw is the plain-text STOMP payload; parse here (dto-layer input handling) so the
    // controller stays pure delegation. Bad input throws → swallowed by the controller's error handler.
    public void sendTyping(String googleId, String recipientUserIdRaw) throws ApiException {
        TypingSignalResult result = chatFlow.composeTyping(googleId, Long.parseLong(recipientUserIdRaw.trim()));
        if (result != null) {
            typingPublisher.publish(result.getRecipientGoogleId(), result.getTyping());
        }
    }

    public List<MessageData> loadMessages(String googleId, Long conversationId, Long beforeId, int size) throws ApiException {
        return chatFlow.loadMessages(googleId, conversationId, beforeId, size).stream()
                .map(view -> ConversionHelper.toMessageData(view.getMessage(), view.getReactions(), view.getReplyTo()))
                .toList();
    }

    public void react(String googleId, Long messageId, String emoji) throws ApiException {
        ReactionResult result = chatFlow.react(googleId, messageId, emoji);
        reactionPublisher.publish(result.getOtherGoogleId(), result.getEvent());
    }

    public void unreact(String googleId, Long messageId) throws ApiException {
        ReactionResult result = chatFlow.unreact(googleId, messageId);
        reactionPublisher.publish(result.getOtherGoogleId(), result.getEvent());
    }

    public void editMessage(String googleId, Long messageId, MessageEditForm form) throws ApiException {
        checkValid(form);
        trim(form);
        MessageUpdateResult result = chatFlow.editMessage(googleId, messageId, form.getBody());
        messageUpdatePublisher.publish(result.getOtherGoogleId(), result.getUpdate());
    }

    public void deleteMessage(String googleId, Long messageId) throws ApiException {
        MessageUpdateResult result = chatFlow.deleteMessage(googleId, messageId);
        messageUpdatePublisher.publish(result.getOtherGoogleId(), result.getUpdate());
    }

    // Process + store an uploaded image, returning its URL (the client then sends it as an IMAGE message).
    public UploadData uploadImage(MultipartFile file) throws ApiException {
        return ConversionHelper.toUploadData(mediaApi.storeImage(file));
    }

    // Store an uploaded voice note, returning its URL (the client then sends it as a VOICE message).
    public UploadData uploadAudio(MultipartFile file) throws ApiException {
        return ConversionHelper.toUploadData(mediaApi.storeAudio(file));
    }

    public List<PinnedMessageData> listPins(String googleId, Long conversationId) throws ApiException {
        return chatFlow.getPinned(googleId, conversationId);
    }

    public void pin(String googleId, Long messageId) throws ApiException {
        PinResult result = chatFlow.pin(googleId, messageId);
        pinPublisher.publish(result.getOtherGoogleId(), result.getEvent());
    }

    public void unpin(String googleId, Long messageId) throws ApiException {
        PinResult result = chatFlow.unpin(googleId, messageId);
        pinPublisher.publish(result.getOtherGoogleId(), result.getEvent());
    }
}
