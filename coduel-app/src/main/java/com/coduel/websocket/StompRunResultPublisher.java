package com.coduel.websocket;

import com.coduel.interfaces.RunResultPublisher;
import com.coduel.model.data.ExecutionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompRunResultPublisher implements RunResultPublisher {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, ExecutionData result) {
        // Routes to /user/queue/run-result for the specific googleId principal.
        messagingTemplate.convertAndSendToUser(googleId, "/queue/run-result", JSON.writeValueAsString(result));
    }
}
