package com.coduel.flow;

import com.coduel.api.MessageApi;
import com.coduel.api.MessageReactionApi;
import com.coduel.api.PinnedMessageApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * One disappearing-thread purge in its OWN transaction, so {@link MessageRetentionFlow}'s sweep commits
 * thread-by-thread and a failure on one thread can't roll back the others. Separate component (not a
 * self-call on the flow) so the @Transactional proxy actually applies.
 */
@Component
public class MessageThreadPurger {

    @Autowired
    private MessageApi messageApi;
    @Autowired
    private MessageReactionApi messageReactionApi;
    @Autowired
    private PinnedMessageApi pinnedMessageApi;

    @Transactional(rollbackFor = Exception.class)
    public int purgeThread(Long conversationId, Instant enabledAt, Instant cutoff) {
        List<Long> ids = messageApi.getExpiredIds(conversationId, enabledAt, cutoff);
        if (ids.isEmpty()) {
            return 0;
        }
        // Children first (no FK cascade), then the messages.
        messageReactionApi.deleteByMessageIds(ids);
        pinnedMessageApi.deleteByMessageIds(ids);
        return messageApi.deleteByIds(ids);
    }
}
