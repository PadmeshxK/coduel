package com.coduel.rabbitmq;

import com.coduel.entity.Submission;
import com.coduel.interfaces.JudgeDispatcher;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@Log4j2
public class JudgeProducer implements JudgeDispatcher {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void dispatch(Submission submission) {

        String routingKey = "submission-" + submission.getUserId();

        Boolean confirmed = rabbitTemplate.invoke(operations -> {
            operations.convertAndSend(RabbitMQConstants.CODUEL_EXCHANGE, routingKey, String.valueOf(submission.getId()));
            return operations.waitForConfirms(15000); // 15-second timeout
        });

        if (Boolean.FALSE.equals(confirmed)) {
            throw new IllegalStateException("RabbitMQ failed to confirm submission " + submission.getId());
        }
        log.debug("Dispatched submission {} for judging (key {})", submission.getId(), routingKey);
    }
}
