package com.coduel.websocket;

import com.coduel.interfaces.UserNotificationPublisher;
import com.coduel.model.data.NotificationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompUserNotificationPublisher implements UserNotificationPublisher {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, NotificationData notification) {
        // Routes to /user/queue/notification for the specific googleId principal.
        messagingTemplate.convertAndSendToUser(googleId, "/queue/notification",
                JSON.writeValueAsString(notification));
    }
}
