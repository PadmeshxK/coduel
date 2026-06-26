package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.dao.PinnedMessageDao;
import com.coduel.entity.PinnedMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = Exception.class)
public class PinnedMessageApi extends AbstractApi {

    @Autowired
    private PinnedMessageDao pinnedMessageDao;

    public List<PinnedMessage> getForConversation(Long conversationId) {
        return pinnedMessageDao.selectForConversation(conversationId);
    }

    public PinnedMessage find(Long conversationId, Long messageId) {
        return pinnedMessageDao.selectForMessage(conversationId, messageId);
    }

    public long countForConversation(Long conversationId) {
        return pinnedMessageDao.countForConversation(conversationId);
    }

    public PinnedMessage save(PinnedMessage pin) {
        return Objects.isNull(pin.getId()) ? pinnedMessageDao.persist(pin) : pin;
    }

    public void delete(PinnedMessage pin) {
        pinnedMessageDao.delete(pin);
    }

    // Bulk-remove pins for purged messages (the disappearing sweep).
    public int deleteByMessageIds(List<Long> messageIds) {
        return pinnedMessageDao.deleteByMessageIds(messageIds);
    }
}
