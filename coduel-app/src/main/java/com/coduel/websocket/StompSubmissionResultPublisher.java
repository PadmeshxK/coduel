package com.coduel.websocket;

import com.coduel.interfaces.SubmissionResultPublisher;
import com.coduel.model.data.SubmissionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class StompSubmissionResultPublisher implements SubmissionResultPublisher {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String googleId, SubmissionData result) {
        // Routes to /user/queue/submission-result for the specific googleId principal.
        messagingTemplate.convertAndSendToUser(googleId, "/queue/submission-result",
                JSON.writeValueAsString(result));
    }
}
