package com.coduel.websocket;

import com.coduel.interfaces.MatchEventPublisher;
import com.coduel.model.data.MatchEventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompMatchEventPublisher implements MatchEventPublisher {

    // Boot 4 ships Jackson 3 only; spring-messaging's default JSON converter is Jackson 2-based and
    // absent, so serialize to a JSON string ourselves and send it (the SPA JSON.parses the frame body).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(Long matchId, MatchEventData event) {
        messagingTemplate.convertAndSend("/topic/match/" + matchId, JSON.writeValueAsString(event));
    }
}
