package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.MessageDao;
import com.coduel.entity.Message;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.MessageKind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class MessageApi extends AbstractApi {

    @Autowired
    private MessageDao messageDao;

    public Message create(Long conversationId, Long senderId, String body, Long replyToId,
                          MessageKind kind, String codeLanguage, String attachmentUrl, String sharedRef,
                          Integer durationMs) {
        return messageDao.persist(ConversionHelper.toMessage(
                conversationId, senderId, body, replyToId, kind, codeLanguage, attachmentUrl, sharedRef, durationMs));
    }

    public List<Message> getByIds(List<Long> ids) {
        return messageDao.selectByIds(ids);
    }

    public Message getCheckById(Long id) throws ApiException {
        Message message = messageDao.selectById(id);
        if (Objects.isNull(message)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_136, List.of(id));
        }
        return message;
    }

    public List<Message> getPage(Long conversationId, Long beforeId, int limit) {
        return messageDao.selectPage(conversationId, beforeId, limit);
    }

    // The next page NEWER than afterId, chronological (for windowed scroll-down).
    public List<Message> getNewerPage(Long conversationId, Long afterId, int limit) {
        return messageDao.selectNewer(conversationId, afterId, limit);
    }

    // The newest message in a thread (null if empty) — used to refresh the inbox snapshot on edit/delete.
    public Message getLatest(Long conversationId) {
        return messageDao.selectLatest(conversationId);
    }

    // Search the caller's conversations for messages whose body contains the query (case-insensitive),
    // newest-first, offset-paginated + a total count (mirrors the problem-list paging).
    public List<Message> searchPage(List<Long> conversationIds, String query, int page, int size) {
        return messageDao.searchPage(conversationIds, "%" + query.toLowerCase() + "%", page, size);
    }

    public long searchCount(List<Long> conversationIds, String query) {
        return messageDao.searchCount(conversationIds, "%" + query.toLowerCase() + "%");
    }

    // Disappearing sweep: ids of messages sent while the option was on (>= enabledAt) now past the cutoff.
    public List<Long> getExpiredIds(Long conversationId, java.time.Instant enabledAt, java.time.Instant cutoff) {
        return messageDao.selectExpiredIds(conversationId, enabledAt, cutoff);
    }

    public int deleteByIds(List<Long> ids) {
        return messageDao.deleteByIds(ids);
    }
}
