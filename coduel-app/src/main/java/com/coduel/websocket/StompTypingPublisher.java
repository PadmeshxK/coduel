package com.coduel.websocket;

import com.coduel.interfaces.TypingPublisher;
import com.coduel.model.data.TypingData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompTypingPublisher implements TypingPublisher {

    // Boot 4 ships Jackson 3 only — serialize to a JSON string ourselves (same as the other publishers).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, TypingData typing) {
        messagingTemplate.convertAndSendToUser(googleId, "/queue/typing", JSON.writeValueAsString(typing));
    }
}
