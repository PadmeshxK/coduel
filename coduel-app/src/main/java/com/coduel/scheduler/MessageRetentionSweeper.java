package com.coduel.scheduler;

import com.coduel.flow.MessageRetentionFlow;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * One sweeper for ALL disappearing threads — every minute it purges messages past their TTL (only those
 * sent while the option was on). DB-driven, so it survives restarts; clients see the gap on next load.
 */
@Component
@Log4j2
public class MessageRetentionSweeper {

    @Autowired
    private MessageRetentionFlow messageRetentionFlow;

    @Scheduled(fixedDelay = 60_000)
    public void sweep() {
        try {
            int purged = messageRetentionFlow.purgeExpired();
            if (purged > 0) {
                log.info("Disappearing sweep purged {} messages", purged);
            }
        } catch (Exception e) {
            log.warn("Disappearing sweep failed: {}", e.getMessage());
        }
    }
}
