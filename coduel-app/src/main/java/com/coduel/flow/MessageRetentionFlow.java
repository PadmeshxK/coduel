package com.coduel.flow;

import com.coduel.api.ConversationApi;
import com.coduel.api.ConversationSettingApi;
import com.coduel.entity.Conversation;
import com.coduel.entity.ConversationSetting;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Disappearing-message retention. For every thread with disappearing enabled, purge the messages that
 * were sent WHILE it was on (createdAt >= the enable instant) and are now older than that side's TTL —
 * history from before it was turned on is left untouched. The actual per-thread delete runs in
 * {@link MessageThreadPurger} in its OWN transaction; this driver loop is deliberately NOT transactional
 * so each thread commits independently and one bad thread can't roll back (or stall) the whole sweep.
 */
@Component
@Log4j2
public class MessageRetentionFlow {

    @Autowired
    private ConversationSettingApi conversationSettingApi;
    @Autowired
    private ConversationApi conversationApi;
    @Autowired
    private MessageThreadPurger messageThreadPurger;

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
            // Each thread is its own transaction — a failure here is logged and the sweep moves on.
            try {
                purged += messageThreadPurger.purgeThread(conversation.getId(), enabledAt, cutoff);
            } catch (RuntimeException e) {
                log.warn("Disappearing purge failed for conversation {}", conversation.getId(), e);
            }
        }
        return purged;
    }
}
