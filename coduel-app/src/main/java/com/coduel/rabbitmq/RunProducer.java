package com.coduel.rabbitmq;

import com.coduel.interfaces.RunDispatcher;
import com.coduel.model.message.RunTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@Log4j2
public class RunProducer implements RunDispatcher {

    // Boot 4 ships Jackson 3 only — serialize the task ourselves (same as the WS publishers).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void dispatch(RunTask task) {
        // Routed per-user; a run is ephemeral, so no publisher-confirm — if it's lost the user re-runs.
        String routingKey = "run-" + task.getGoogleId();
        rabbitTemplate.convertAndSend(RabbitMQConstants.RUN_EXCHANGE, routingKey, JSON.writeValueAsString(task));
        log.debug("Dispatched run {} (key {})", task.getRunId(), routingKey);
    }
}
