package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.dao.MessageReactionDao;
import com.coduel.entity.MessageReaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = Exception.class)
public class MessageReactionApi extends AbstractApi {

    @Autowired
    private MessageReactionDao messageReactionDao;

    public MessageReaction find(Long messageId, Long userId) {
        return messageReactionDao.selectForMessageAndUser(messageId, userId);
    }

    public List<MessageReaction> getForMessages(List<Long> messageIds) {
        return messageReactionDao.selectForMessages(messageIds);
    }

    public MessageReaction save(MessageReaction reaction) {
        return Objects.isNull(reaction.getId()) ? messageReactionDao.persist(reaction) : reaction;
    }

    public void delete(MessageReaction reaction) {
        messageReactionDao.delete(reaction);
    }

    // Bulk-remove reactions for purged messages (the disappearing sweep).
    public int deleteByMessageIds(List<Long> messageIds) {
        return messageReactionDao.deleteByMessageIds(messageIds);
    }
}
