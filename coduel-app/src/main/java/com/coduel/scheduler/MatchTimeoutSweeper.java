package com.coduel.scheduler;

import com.coduel.api.MatchApi;
import com.coduel.entity.Match;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.MatchEventPublisher;
import com.coduel.model.constant.MatchEndReason;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * One sweeper for ALL matches (not a thread per match): every minute it asks the DB for ACTIVE
 * matches older than the TTL and expires them. The DB is the source of truth, so this survives
 * restarts. Connected players are notified live via a MATCH_OVER event with no winner.
 */
@Component
@Log4j2
public class MatchTimeoutSweeper {

    // Hard problems can take a while — give a match a full hour before timing it out.
    private static final long MATCH_TTL_MINUTES = 60;

    @Autowired
    private MatchApi matchApi;
    @Autowired
    private MatchEventPublisher matchEventPublisher;

    @Scheduled(fixedDelay = 60_000)
    public void expireStaleMatches() {
        Instant cutoff = Instant.now().minus(MATCH_TTL_MINUTES, ChronoUnit.MINUTES);
        List<Match> stale = matchApi.getActiveOlderThan(cutoff);
        for (Match match : stale) {
            try {
                // expire() is idempotent (no-op if it already finished/expired).
                if (matchApi.expire(match.getId(), MatchEndReason.TIMEOUT)) {
                    matchEventPublisher.publish(match.getId(),
                            ConversionHelper.toMatchOverEvent(null, MatchEndReason.TIMEOUT));
                    log.info("Match {} expired after {} min", match.getId(), MATCH_TTL_MINUTES);
                }
            } catch (Exception e) {
                log.warn("Failed to expire match {}: {}", match.getId(), e.getMessage());
            }
        }
    }
}
