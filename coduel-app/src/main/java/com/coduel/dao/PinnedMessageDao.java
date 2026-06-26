package com.coduel.dao;

import com.coduel.entity.PinnedMessage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PinnedMessageDao extends BaseDao<PinnedMessage> {

    private static final String SELECT_FOR_CONVERSATION =
            "SELECT p FROM PinnedMessage p WHERE p.conversationId = :c ORDER BY p.id DESC";
    private static final String SELECT_FOR_MESSAGE =
            "SELECT p FROM PinnedMessage p WHERE p.conversationId = :c AND p.messageId = :m";
    private static final String COUNT_FOR_CONVERSATION =
            "SELECT COUNT(p) FROM PinnedMessage p WHERE p.conversationId = :c";
    private static final String DELETE_FOR_MESSAGES =
            "DELETE FROM PinnedMessage p WHERE p.messageId IN :ids";

    public PinnedMessageDao() {
        super(PinnedMessage.class);
    }

    public int deleteByMessageIds(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return 0;
        }
        return em().createQuery(DELETE_FOR_MESSAGES).setParameter("ids", messageIds).executeUpdate();
    }

    public List<PinnedMessage> selectForConversation(Long conversationId) {
        return createQuery(SELECT_FOR_CONVERSATION, PinnedMessage.class)
                .setParameter("c", conversationId)
                .getResultList();
    }

    public PinnedMessage selectForMessage(Long conversationId, Long messageId) {
        return selectSingleOrNull(createQuery(SELECT_FOR_MESSAGE, PinnedMessage.class)
                .setParameter("c", conversationId)
                .setParameter("m", messageId));
    }

    public long countForConversation(Long conversationId) {
        return em().createQuery(COUNT_FOR_CONVERSATION, Long.class)
                .setParameter("c", conversationId)
                .getSingleResult();
    }
}
