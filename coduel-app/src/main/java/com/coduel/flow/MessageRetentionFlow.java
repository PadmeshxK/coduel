package com.coduel.flow;

import com.coduel.api.ConversationApi;
import com.coduel.api.ConversationSettingApi;
import com.coduel.api.MessageApi;
import com.coduel.api.MessageReactionApi;
import com.coduel.api.PinnedMessageApi;
import com.coduel.entity.Conversation;
import com.coduel.entity.ConversationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Disappearing-message retention. For every thread with disappearing enabled, purge the messages that
 * were sent WHILE it was on (createdAt >= the enable instant) and are now older than that side's TTL —
 * history from before it was turned on is left untouched. Cross-Api orchestration, so it lives in a
 * flow: it cleans each purged message's reactions + pins before removing the messages themselves.
 */
@Component
@Transactional(rollbackFor = Exception.class)
public class MessageRetentionFlow {

    @Autowired
    private ConversationSettingApi conversationSettingApi;
    @Autowired
    private ConversationApi conversationApi;
    @Autowired
    private MessageApi messageApi;
    @Autowired
    private MessageReactionApi messageReactionApi;
    @Autowired
    private PinnedMessageApi pinnedMessageApi;

    public int purgeExpired() {
        int purged = 0;
        Instant now = Instant.now();
        for (ConversationSetting setting : conversationSettingApi.getWithDisappearing()) {
            Integer ttl = setting.getDisappearingTtlSeconds();
            Instant enabledAt = setting.getDisappearingEnabledAt();
            // Need both a positive TTL and a known enable instant (older rows without one are skipped).
            if (ttl == null || ttl <= 0 || enabledAt == null) {
                continue;
            }
            Conversation conversation = conversationApi.find(setting.getOwnerUserId(), setting.getPeerUserId());
            if (conversation == null) {
                continue;
            }
            Instant cutoff = now.minusSeconds(ttl);
            List<Long> ids = messageApi.getExpiredIds(conversation.getId(), enabledAt, cutoff);
            if (ids.isEmpty()) {
                continue;
            }
            // Children first (no FK cascade), then the messages.
            messageReactionApi.deleteByMessageIds(ids);
            pinnedMessageApi.deleteByMessageIds(ids);
            purged += messageApi.deleteByIds(ids);
        }
        return purged;
    }
}
