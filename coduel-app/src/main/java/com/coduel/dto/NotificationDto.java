package com.coduel.dto;

import com.coduel.common.exception.ApiException;
import com.coduel.flow.NotificationFlow;
import com.coduel.interfaces.NotificationInbox;
import com.coduel.model.data.NotificationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationDto {

    @Autowired
    private NotificationFlow notificationFlow;
    @Autowired
    private NotificationInbox notificationInbox;

    // The inbox (Redis) is the external call → it lives here; the flow does the DB-backed assembly.
    public List<NotificationData> getPendingNotifications(String googleId) throws ApiException {
        List<NotificationData> inbox = notificationInbox.getAll(googleId);
        return notificationFlow.getPendingNotifications(googleId, inbox);
    }
}
