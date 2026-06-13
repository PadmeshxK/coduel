package com.coduel.websocket;

import com.coduel.interfaces.MatchEventPublisher;
import com.coduel.model.data.MatchEventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompMatchEventPublisher implements MatchEventPublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(Long matchId, MatchEventData event) {
        messagingTemplate.convertAndSend("/topic/match/" + matchId, event);
    }
}
