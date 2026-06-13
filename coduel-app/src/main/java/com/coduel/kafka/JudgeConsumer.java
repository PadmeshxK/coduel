package com.coduel.kafka;

import com.coduel.dto.JudgeDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class JudgeConsumer {

    @Autowired
    private JudgeDto judgeDto;

    // Inbound Kafka entry point (the message-driven counterpart to a Controller): parse the
    // submissionId off the message and delegate to JudgeDto. Holds no judging logic itself.
    @KafkaListener(topics = KafkaTopics.JUDGE_REQUESTS, groupId = KafkaWorkers.JUDGE_CONSUMER)
    public void onJudgeRequest(String submissionId) {
        try {
            judgeDto.judge(Long.parseLong(submissionId));
        } catch (Exception e) {
            // A consumer must handle its own errors — they can't propagate back to an HTTP caller.
            log.error("Failed to judge submission {}", submissionId, e);
        }
    }
}
