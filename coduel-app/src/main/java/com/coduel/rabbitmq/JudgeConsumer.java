package com.coduel.rabbitmq;

import com.coduel.dto.JudgeDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class JudgeConsumer {

    @Autowired
    private JudgeDto judgeDto;

    // Inbound RabbitMQ entry point (the message-driven counterpart to a Controller): parse the
    // submissionId off the message and delegate to JudgeDto. Holds no judging logic itself.
    @RabbitListener(queues = RabbitMQConstants.SUBMISSION_QUEUE, concurrency = "3")
    public void onSubmissionRequest(String submissionId) {
        try {
            log.debug("Worker picked up submission {} from RabbitMQ", submissionId);

            // Hand off to the exact same judging orchestration flow you already built
            judgeDto.judge(Long.parseLong(submissionId));

        } catch (Exception e) {
            // Do NOT swallow: rethrow so the listener's retry interceptor reattempts (for transient
            // infra failures) and, on exhaustion, the message is dead-lettered instead of silently
            // dropped. Re-judging is idempotent (already-judged → no-op), so retries are safe.
            log.error("Judging failed for submission {} — will retry, then dead-letter", submissionId, e);
            throw new AmqpException("Judging failed for submission " + submissionId, e);
        }
    }
}
