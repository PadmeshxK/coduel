package com.coduel.dto;

import com.coduel.common.exception.ApiException;
import com.coduel.flow.NotificationFlow;
import com.coduel.model.data.NotificationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationDto {

    @Autowired
    private NotificationFlow notificationFlow;

    public List<NotificationData> getPendingNotifications(String googleId) throws ApiException {
        return notificationFlow.getPendingNotifications(googleId);
    }
}
