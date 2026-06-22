package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.ConversationDao;
import com.coduel.entity.Conversation;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class ConversationApi extends AbstractApi {

    private static final int PREVIEW_MAX = 200;

    @Autowired
    private ConversationDao conversationDao;

    public Conversation getCheckById(Long id) throws ApiException {
        Conversation conversation = conversationDao.selectById(id);
        if (Objects.isNull(conversation)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_132, List.of(id));
        }
        return conversation;
    }

    // Find — or create — the 1:1 thread for a user pair. Keyed by the sorted pair (low <= high) so it's
    // direction-independent and unique; one row per pair.
    public Conversation getOrCreate(Long userA, Long userB) {
        Long low = Math.min(userA, userB);
        Long high = Math.max(userA, userB);
        Conversation existing = conversationDao.selectBetween(low, high);
        return Objects.nonNull(existing)
                ? existing
                : conversationDao.persist(ConversionHelper.toConversation(low, high));
    }

    // Refresh the denormalized "last message" snapshot the inbox renders from (dirty-checked on commit).
    public void recordLastMessage(Conversation conversation, Long senderId, String body) {
        conversation.setLastMessageAt(Instant.now());
        conversation.setLastSenderId(senderId);
        conversation.setLastPreview(body.length() <= PREVIEW_MAX ? body : body.substring(0, PREVIEW_MAX));
    }

    public List<Conversation> getForUser(Long userId) {
        return conversationDao.selectForUser(userId);
    }

    // Mark the thread read up to now for this participant (dirty-checked on commit). Persisted, so the
    // read state survives leaving and re-entering the thread — and reloads on another device.
    public void markRead(Conversation conversation, Long userId) {
        Instant now = Instant.now();
        if (userId.equals(conversation.getLowerUserId())) {
            conversation.setLowerUserLastReadAt(now);
        } else {
            conversation.setHigherUserLastReadAt(now);
        }
    }

    // Unread for this viewer = there's a last message, it wasn't theirs, and it's newer than their read
    // marker (or they've never opened the thread).
    public boolean isUnreadFor(Conversation conversation, Long userId) {
        Instant lastMessageAt = conversation.getLastMessageAt();
        if (lastMessageAt == null || userId.equals(conversation.getLastSenderId())) {
            return false;
        }
        Instant lastRead = userId.equals(conversation.getLowerUserId())
                ? conversation.getLowerUserLastReadAt()
                : conversation.getHigherUserLastReadAt();
        return lastRead == null || lastMessageAt.isAfter(lastRead);
    }
}
