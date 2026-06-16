package com.coduel.rabbitmq;

import com.coduel.dto.JudgeDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A submission whose judging exhausted all retries lands on the dead-letter queue. Resolve it to a
 * terminal INTERNAL_ERROR and push the result, so the user stops waiting within the retry window
 * (~seconds) rather than the orphan-sweeper backstop (minutes).
 *
 * We do NOT rethrow here: a failure to resolve a dead-lettered message must not bounce it around.
 * If this can't run (e.g. DB down), the SubmissionTimeoutSweeper is the backstop.
 */
@Component
@Log4j2
public class SubmissionDeadLetterConsumer {

    @Autowired
    private JudgeDto judgeDto;

    @RabbitListener(queues = RabbitMQConstants.DEAD_LETTER_QUEUE)
    public void onDeadLetter(String submissionId) {
        log.error("Submission {} dead-lettered — resolving as INTERNAL_ERROR", submissionId);
        try {
            judgeDto.markInternalError(Long.parseLong(submissionId));
        } catch (Exception e) {
            log.error("Could not resolve dead-lettered submission {} (orphan sweeper will retry)",
                    submissionId, e);
        }
    }
}
