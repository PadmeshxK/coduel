package com.coduel.dao;

import com.coduel.entity.MessageReaction;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageReactionDao extends BaseDao<MessageReaction> {

    private static final String SELECT_FOR_MESSAGE_USER =
            "SELECT r FROM MessageReaction r WHERE r.messageId = :message AND r.userId = :user";
    // Batch-load every reaction on a page of messages, so decorating a thread isn't an N+1.
    private static final String SELECT_FOR_MESSAGES =
            "SELECT r FROM MessageReaction r WHERE r.messageId IN :ids";
    private static final String DELETE_FOR_MESSAGES =
            "DELETE FROM MessageReaction r WHERE r.messageId IN :ids";

    public MessageReactionDao() {
        super(MessageReaction.class);
    }

    public int deleteByMessageIds(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return 0;
        }
        return em().createQuery(DELETE_FOR_MESSAGES).setParameter("ids", messageIds).executeUpdate();
    }

    public MessageReaction selectForMessageAndUser(Long messageId, Long userId) {
        return selectSingleOrNull(createQuery(SELECT_FOR_MESSAGE_USER, MessageReaction.class)
                .setParameter("message", messageId)
                .setParameter("user", userId));
    }

    public List<MessageReaction> selectForMessages(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return List.of(); // an empty IN (:ids) is invalid JPQL — short-circuit
        }
        return createQuery(SELECT_FOR_MESSAGES, MessageReaction.class)
                .setParameter("ids", messageIds)
                .getResultList();
    }
}
