package com.coduel.dto;

import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.flow.ChatFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.DmPublisher;
import com.coduel.interfaces.UserNotificationPublisher;
import com.coduel.model.data.ConversationData;
import com.coduel.model.data.MessageData;
import com.coduel.model.form.MessageForm;
import com.coduel.model.result.DmSentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatDto extends AbstractDto {

    @Autowired
    private ChatFlow chatFlow;
    @Autowired
    private DmPublisher dmPublisher;
    @Autowired
    private UserNotificationPublisher userNotificationPublisher;

    public MessageData sendMessage(String googleId, MessageForm form) throws ApiException {
        checkValid(form);
        trim(form);
        DmSentResult result = chatFlow.sendDirectMessage(googleId, form.getRecipientUserId(), form.getBody());
        MessageData data = ConversionHelper.toMessageData(result.getMessage());
        // Two channels: the message itself for the live thread, and a notification so the recipient is
        // alerted even when they're not on the Messages page.
        dmPublisher.publish(result.getRecipientGoogleId(), data);
        userNotificationPublisher.publish(result.getRecipientGoogleId(),
                ConversionHelper.toDmNotification(result.getSender()));
        return data; // echoed back to the sender as the HTTP response
    }

    public List<ConversationData> listConversations(String googleId) throws ApiException {
        return chatFlow.listConversations(googleId).stream()
                .map(view -> ConversionHelper.toConversationData(view.getConversation(), view.getOther(), view.isUnread()))
                .toList();
    }

    public void markRead(String googleId, Long conversationId) throws ApiException {
        chatFlow.markRead(googleId, conversationId);
    }

    public List<MessageData> loadMessages(String googleId, Long conversationId, Long beforeId, int size) throws ApiException {
        return chatFlow.loadMessages(googleId, conversationId, beforeId, size).stream()
                .map(ConversionHelper::toMessageData)
                .toList();
    }
}
