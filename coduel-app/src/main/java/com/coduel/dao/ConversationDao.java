package com.coduel.dao;

import com.coduel.entity.Conversation;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ConversationDao extends BaseDao<Conversation> {

    private static final String SELECT_BETWEEN =
            "SELECT c FROM Conversation c WHERE c.lowerUserId = :lower AND c.higherUserId = :higher";
    // Every thread a user is in, most-recently-active first — the inbox.
    private static final String SELECT_FOR_USER =
            "SELECT c FROM Conversation c WHERE c.lowerUserId = :u OR c.higherUserId = :u ORDER BY c.lastMessageAt DESC";

    public ConversationDao() {
        super(Conversation.class);
    }

    public Conversation selectBetween(Long lower, Long higher) {
        return selectSingleOrNull(createQuery(SELECT_BETWEEN, Conversation.class)
                .setParameter("lower", lower)
                .setParameter("higher", higher));
    }

    public List<Conversation> selectForUser(Long userId) {
        return createQuery(SELECT_FOR_USER, Conversation.class)
                .setParameter("u", userId)
                .getResultList();
    }
}
