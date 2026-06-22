package com.coduel.rabbitmq;

import com.coduel.dto.RunDto;
import com.coduel.model.message.RunTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@Log4j2
public class RunConsumer {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private RunDto runDto;

    // Inbound entry point for the run queue (the message-driven counterpart to a controller). A run is
    // ephemeral with no DLQ — RunDto handles its own failures by pushing an error result to the user,
    // so we never rethrow (rethrowing would just requeue a doomed message).
    @RabbitListener(queues = RabbitMQConstants.RUN_QUEUE, concurrency = "3")
    public void onRunRequest(String payload) {
        try {
            runDto.run(JSON.readValue(payload, RunTask.class));
        } catch (Exception e) {
            // Malformed payload or an unexpected error — drop it; there's no user to notify reliably.
            log.error("Dropping unprocessable run message", e);
        }
    }
}
