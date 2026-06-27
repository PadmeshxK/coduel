package com.coduel.dao;

import com.coduel.entity.Message;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class MessageDao extends BaseDao<Message> {

    // Keyset pagination, newest-first — optionally older than a cursor id (scroll up for history).
    private static final String SELECT_PAGE =
            "SELECT m FROM Message m WHERE m.conversationId = :c ORDER BY m.id DESC";
    private static final String SELECT_PAGE_BEFORE =
            "SELECT m FROM Message m WHERE m.conversationId = :c AND m.id < :before ORDER BY m.id DESC";
    // Forward pagination — the next NEWER messages after a cursor, oldest-first (for windowed scroll-down).
    private static final String SELECT_PAGE_AFTER =
            "SELECT m FROM Message m WHERE m.conversationId = :c AND m.id > :after ORDER BY m.id ASC";
    // Batch-fetch the targets of replies on a page, to build their quoted previews in one query.
    private static final String SELECT_BY_IDS = "SELECT m FROM Message m WHERE m.id IN :ids";
    // Disappearing-message sweep: ids of messages SENT WHILE the option was enabled (createdAt >= enabledAt)
    // that are now older than the cutoff — so history from before it was turned on is never touched.
    private static final String SELECT_EXPIRED_IDS =
            "SELECT m.id FROM Message m WHERE m.conversationId = :c "
                    + "AND m.createdAt >= :enabledAt AND m.createdAt < :cutoff";
    private static final String DELETE_BY_IDS = "DELETE FROM Message m WHERE m.id IN :ids";

    public MessageDao() {
        super(Message.class);
    }

    public List<Long> selectExpiredIds(Long conversationId, Instant enabledAt, Instant cutoff) {
        return createQuery(SELECT_EXPIRED_IDS, Long.class)
                .setParameter("c", conversationId)
                .setParameter("enabledAt", enabledAt)
                .setParameter("cutoff", cutoff)
                .getResultList();
    }

    public int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        return em().createQuery(DELETE_BY_IDS).setParameter("ids", ids).executeUpdate();
    }

    public List<Message> selectByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return createQuery(SELECT_BY_IDS, Message.class).setParameter("ids", ids).getResultList();
    }

    // Full-text-ish search over the caller's conversations: non-deleted messages whose body matches,
    // newest-first, offset-paginated + a total count (same shape as the problem list). body holds text,
    // code source, and image/voice captions.
    private static final String SEARCH =
            "SELECT m FROM Message m WHERE m.conversationId IN :ids AND m.deletedAt IS NULL "
                    + "AND LOWER(m.body) LIKE :q ORDER BY m.id DESC";
    private static final String SEARCH_COUNT =
            "SELECT COUNT(m) FROM Message m WHERE m.conversationId IN :ids AND m.deletedAt IS NULL "
                    + "AND LOWER(m.body) LIKE :q";

    public List<Message> searchPage(List<Long> conversationIds, String like, int page, int size) {
        if (conversationIds.isEmpty()) {
            return List.of();
        }
        return createQuery(SEARCH, Message.class)
                .setParameter("ids", conversationIds)
                .setParameter("q", like)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long searchCount(List<Long> conversationIds, String like) {
        if (conversationIds.isEmpty()) {
            return 0;
        }
        return em().createQuery(SEARCH_COUNT, Long.class)
                .setParameter("ids", conversationIds)
                .setParameter("q", like)
                .getSingleResult();
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

    // Next page of messages newer than afterId, oldest-first (chronological).
    public List<Message> selectNewer(Long conversationId, Long afterId, int limit) {
        return createQuery(SELECT_PAGE_AFTER, Message.class)
                .setParameter("c", conversationId)
                .setParameter("after", afterId)
                .setMaxResults(limit)
                .getResultList();
    }

    // The newest message in a thread (highest id) — null if the thread is empty. Drives the inbox
    // snapshot refresh when the newest message is edited/deleted.
    public Message selectLatest(Long conversationId) {
        return selectSingleOrNull(createQuery(SELECT_PAGE, Message.class)
                .setParameter("c", conversationId)
                .setMaxResults(1));
    }
}
