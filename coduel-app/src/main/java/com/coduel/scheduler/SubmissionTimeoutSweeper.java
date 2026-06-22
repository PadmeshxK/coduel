package com.coduel.scheduler;

import com.coduel.api.SubmissionApi;
import com.coduel.dto.JudgeDto;
import com.coduel.entity.Submission;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Backstop, not the primary failure path: the dead-letter consumer resolves *known* judge failures
 * fast. This catches the rarer ORPHAN — a submission whose message was never delivered/processed at
 * all (broker down, worker died without nacking), so it never reached the dead-letter queue.
 * Anything still PENDING past the threshold is force-failed to INTERNAL_ERROR so the user always
 * gets a terminal answer. The threshold is deliberately larger than the consumer retry window, so
 * it never races a submission that's legitimately still being retried.
 */
@Component
@Log4j2
public class SubmissionTimeoutSweeper {

    private static final long ORPHAN_THRESHOLD_MINUTES = 3;

    @Autowired
    private SubmissionApi submissionApi;
    @Autowired
    private JudgeDto judgeDto;

    @Scheduled(fixedDelay = 60_000)
    public void sweepOrphans() {
        Instant cutoff = Instant.now().minus(ORPHAN_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        List<Submission> orphans = submissionApi.getPendingOlderThan(cutoff);
        for (Submission submission : orphans) {
            try {
                // Idempotent: a no-op if it has since been judged.
                judgeDto.markInternalError(submission.getId());
            } catch (Exception e) {
                log.warn("Orphan sweep failed for submission {}: {}", submission.getId(), e.getMessage());
            }
        }
    }
}
