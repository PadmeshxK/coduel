package com.coduel.dao;

import com.coduel.entity.Message;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MessageDao extends BaseDao<Message> {

    // Keyset pagination, newest-first — optionally older than a cursor id (scroll up for history).
    private static final String SELECT_PAGE =
            "SELECT m FROM Message m WHERE m.conversationId = :c ORDER BY m.id DESC";
    private static final String SELECT_PAGE_BEFORE =
            "SELECT m FROM Message m WHERE m.conversationId = :c AND m.id < :before ORDER BY m.id DESC";

    public MessageDao() {
        super(Message.class);
    }

    public List<Message> selectPage(Long conversationId, Long beforeId, int limit) {
        TypedQuery<Message> query = createQuery(beforeId == null ? SELECT_PAGE : SELECT_PAGE_BEFORE, Message.class)
                .setParameter("c", conversationId)
                .setMaxResults(limit);
        if (beforeId != null) {
            query.setParameter("before", beforeId);
        }
        return query.getResultList();
    }
}
