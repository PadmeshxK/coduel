package com.coduel.websocket;

import com.coduel.interfaces.ReactionPublisher;
import com.coduel.model.data.ReactionEventData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompReactionPublisher implements ReactionPublisher {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, ReactionEventData event) {
        // Routes to /user/queue/dm-reaction for the other participant's principal.
        messagingTemplate.convertAndSendToUser(googleId, "/queue/dm-reaction", JSON.writeValueAsString(event));
    }
}
