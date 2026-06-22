package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.NotificationDto;
import com.coduel.model.data.NotificationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationDto notificationDto;

    // Everything pending for the signed-in user (friend requests + live room invites), recent first.
    @GetMapping
    public List<NotificationData> pending(@AuthenticationPrincipal OidcUser principal) throws ApiException {
        return notificationDto.getPendingNotifications(principal.getSubject());
    }
}
