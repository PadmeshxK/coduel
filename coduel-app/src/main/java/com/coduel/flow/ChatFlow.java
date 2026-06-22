package com.coduel.flow;

import com.coduel.api.ConversationApi;
import com.coduel.api.FriendshipApi;
import com.coduel.api.MessageApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Conversation;
import com.coduel.entity.Friendship;
import com.coduel.entity.Message;
import com.coduel.entity.User;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.FriendshipStatus;
import com.coduel.model.result.ConversationView;
import com.coduel.model.result.DmSentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Transactional(rollbackFor = ApiException.class)
public class ChatFlow {

    @Autowired
    private UserApi userApi;
    @Autowired
    private FriendshipApi friendshipApi;
    @Autowired
    private ConversationApi conversationApi;
    @Autowired
    private MessageApi messageApi;

    // Orchestration only: resolve both parties and guard (not self, must be friends), then let the Apis
    // create the message and refresh the thread. The Dto pushes the result to the recipient after commit.
    public DmSentResult sendDirectMessage(String senderGoogleId, Long recipientUserId, String body) throws ApiException {
        User sender = userApi.getCheckByGoogleId(senderGoogleId);
        if (sender.getId().equals(recipientUserId)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_135, List.of());
        }
        Friendship friendship = friendshipApi.findBetween(sender.getId(), recipientUserId);
        if (Objects.isNull(friendship) || friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_134, List.of());
        }
        User recipient = userApi.getCheckById(recipientUserId);

        Conversation conversation = conversationApi.getOrCreate(sender.getId(), recipientUserId);
        Message message = messageApi.create(conversation.getId(), sender.getId(), body);
        conversationApi.recordLastMessage(conversation, sender.getId(), body);

        return ConversionHelper.toDmSentResult(message, recipient.getGoogleId(), sender);
    }

    public List<ConversationView> listConversations(String googleId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        List<ConversationView> views = new ArrayList<>();
        for (Conversation conversation : conversationApi.getForUser(me)) {
            Long otherId = conversation.getLowerUserId().equals(me) ? conversation.getHigherUserId() : conversation.getLowerUserId();
            boolean unread = conversationApi.isUnreadFor(conversation, me);
            views.add(ConversionHelper.toConversationView(conversation, userApi.getCheckById(otherId), unread));
        }
        return views;
    }

    // Mark a thread read for the caller (participants only) — persists their read marker.
    public void markRead(String googleId, Long conversationId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Conversation conversation = conversationApi.getCheckById(conversationId);
        if (!conversation.getLowerUserId().equals(me) && !conversation.getHigherUserId().equals(me)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_133, List.of(conversationId));
        }
        conversationApi.markRead(conversation, me);
    }

    public List<Message> loadMessages(String googleId, Long conversationId, Long beforeId, int limit) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Conversation conversation = conversationApi.getCheckById(conversationId);
        if (!conversation.getLowerUserId().equals(me) && !conversation.getHigherUserId().equals(me)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_133, List.of(conversationId));
        }
        return messageApi.getPage(conversationId, beforeId, limit);
    }
}
