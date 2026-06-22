package com.coduel.websocket;

import com.coduel.interfaces.RoomChatPublisher;
import com.coduel.model.data.RoomChatData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompRoomChatPublisher implements RoomChatPublisher {

    // Boot 4 ships Jackson 3 only — serialize to a JSON string ourselves (same as the other publishers).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(Long roomId, RoomChatData message) {
        // A sub-topic of the room channel; the subscription interceptor gates it on room membership.
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/chat", JSON.writeValueAsString(message));
    }
}
