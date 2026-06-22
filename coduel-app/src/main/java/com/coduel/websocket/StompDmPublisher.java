package com.coduel.websocket;

import com.coduel.interfaces.DmPublisher;
import com.coduel.model.data.MessageData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompDmPublisher implements DmPublisher {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, MessageData message) {
        // Routes to /user/queue/dm for the recipient's principal.
        messagingTemplate.convertAndSendToUser(googleId, "/queue/dm", JSON.writeValueAsString(message));
    }
}
