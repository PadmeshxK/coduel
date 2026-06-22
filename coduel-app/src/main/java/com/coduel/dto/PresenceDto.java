package com.coduel.dto;

import com.coduel.api.UserApi;
import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.websocket.PresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PresenceDto extends AbstractDto {

    @Autowired
    private UserApi userApi;
    @Autowired
    private PresenceService presenceService;

    /** Which of my friends are online right now — the client seeds its presence set with this on load. */
    public List<Long> getOnlineFriendIds(String googleId) throws ApiException {
        Long userId = userApi.getCheckByGoogleId(googleId).getId();
        return presenceService.getOnlineFriendIds(userId);
    }
}
