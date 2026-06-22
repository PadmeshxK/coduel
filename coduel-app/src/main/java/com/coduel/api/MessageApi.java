package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.MessageDao;
import com.coduel.entity.Message;
import com.coduel.helper.ConversionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = ApiException.class)
public class MessageApi extends AbstractApi {

    @Autowired
    private MessageDao messageDao;

    public Message create(Long conversationId, Long senderId, String body) {
        return messageDao.persist(ConversionHelper.toMessage(conversationId, senderId, body));
    }

    public List<Message> getPage(Long conversationId, Long beforeId, int limit) {
        return messageDao.selectPage(conversationId, beforeId, limit);
    }
}
