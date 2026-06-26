package com.coduel.websocket;

import com.coduel.interfaces.ChatReadPublisher;
import com.coduel.model.data.ReadReceiptData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompChatReadPublisher implements ChatReadPublisher {

    // Boot 4 ships Jackson 3 only — serialize to a JSON string ourselves (same as the other publishers).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, ReadReceiptData receipt) {
        messagingTemplate.convertAndSendToUser(googleId, "/queue/chat-read", JSON.writeValueAsString(receipt));
    }
}
