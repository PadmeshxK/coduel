package com.coduel.websocket;

import com.coduel.interfaces.MessageUpdatePublisher;
import com.coduel.model.data.MessageUpdateData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompMessageUpdatePublisher implements MessageUpdatePublisher {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, MessageUpdateData update) {
        // Routes to /user/queue/dm-update for the other participant's principal.
        messagingTemplate.convertAndSendToUser(googleId, "/queue/dm-update", JSON.writeValueAsString(update));
    }
}
