package com.coduel.kafka;

import com.coduel.entity.Submission;
import com.coduel.interfaces.JudgeDispatcher;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Component
@Log4j2
public class JudgeProducer implements JudgeDispatcher {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void dispatch(Submission submission) {
        // Key by matchId so every submission in a match lands on the same partition and is
        // judged in receipt order; solo submissions key by their own id (order irrelevant).
        String key = Objects.nonNull(submission.getMatchId())
                ? "match-" + submission.getMatchId()
                : "submission-" + submission.getId();

        // Synchronous send: block on the broker ack so any failure throws here, inside the caller's
        // transaction, rolling back the submission persist. Payload is just the submissionId.
        try {
            kafkaTemplate.send(KafkaTopics.JUDGE_REQUESTS, key, String.valueOf(submission.getId())).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted dispatching submission " + submission.getId(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to dispatch submission " + submission.getId(), e.getCause());
        }
        log.debug("Dispatched submission {} for judging (key {})", submission.getId(), key);
    }
}
