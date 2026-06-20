package com.coduel.dto;

import com.coduel.common.exception.ApiException;
import com.coduel.flow.FriendFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.UserNotificationPublisher;
import com.coduel.model.data.FriendData;
import com.coduel.model.data.FriendRequestData;
import com.coduel.model.result.FriendRequestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FriendDto {

    @Autowired
    private FriendFlow friendFlow;
    @Autowired
    private UserNotificationPublisher userNotificationPublisher;

    public void sendRequest(String googleId, Long targetUserId) throws ApiException {
        FriendRequestResult result = friendFlow.sendRequest(googleId, targetUserId);
        // Publish post-transaction so the request is committed before the push fires.
        userNotificationPublisher.publish(
                result.getAddressee().getGoogleId(),
                ConversionHelper.toFriendRequestNotification(result.getFriendship(), result.getRequester()));
    }

    public void accept(String googleId, Long requestId) throws ApiException {
        friendFlow.accept(googleId, requestId);
    }

    public void remove(String googleId, Long requestId) throws ApiException {
        friendFlow.remove(googleId, requestId);
    }

    public void unfriend(String googleId, Long otherUserId) throws ApiException {
        friendFlow.unfriend(googleId, otherUserId);
    }

    public List<FriendData> listFriends(String googleId) throws ApiException {
        return friendFlow.listFriends(googleId).stream().map(ConversionHelper::toFriendData).toList();
    }

    public List<FriendRequestData> listIncoming(String googleId) throws ApiException {
        return friendFlow.listIncoming(googleId).stream().map(ConversionHelper::toFriendRequestData).toList();
    }
}
