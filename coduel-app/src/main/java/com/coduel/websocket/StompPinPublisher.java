package com.coduel.websocket;

import com.coduel.interfaces.PinPublisher;
import com.coduel.model.data.PinEventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompPinPublisher implements PinPublisher {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, PinEventData event) {
        // Routes to /user/queue/dm-pin for the other participant's principal.
        messagingTemplate.convertAndSendToUser(googleId, "/queue/dm-pin", JSON.writeValueAsString(event));
    }
}
