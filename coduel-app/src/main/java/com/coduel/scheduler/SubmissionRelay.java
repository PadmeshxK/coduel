package com.coduel.scheduler;

import com.coduel.api.SubmissionApi;
import com.coduel.entity.Submission;
import com.coduel.interfaces.JudgeDispatcher;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Transactional-outbox relay. Submissions are persisted with dispatched=false (atomic with the row);
 * this sweep turns each into a Kafka judge request and flips dispatched=true only once the broker has
 * accepted it. Benefits over dispatching inline:
 *  - no dual-write race — the judge only ever reads committed rows,
 *  - no orphan PENDING — a never-sent submission is retried on the next sweep (at-least-once).
 * Pairs with an idempotent judge (re-judging an already-judged submission is a no-op), so the rare
 * double-dispatch (sent ok but mark failed) is harmless. One instance only for now; scale-out would
 * add an atomic claim (UPDATE … WHERE dispatched=false).
 */
@Component
@Log4j2
public class SubmissionRelay {

    private static final int BATCH = 100;

    @Autowired
    private SubmissionApi submissionApi;
    @Autowired
    private JudgeDispatcher judgeDispatcher;

    @Scheduled(fixedDelay = 1000)
    public void relayPending() {
        List<Submission> pending = submissionApi.getUndispatched(BATCH);
        for (Submission submission : pending) {
            try {
                judgeDispatcher.dispatch(submission); // synchronous send
                submissionApi.markDispatched(submission.getId());
            } catch (Exception e) {
                // Leave dispatched=false -> retried next sweep. Broker is likely down, so stop here
                // instead of hammering the whole batch.
                log.warn("Relay failed for submission {}: {}", submission.getId(), e.getMessage());
                break;
            }
        }
    }
}
