package com.coduel.dto;

import com.coduel.common.exception.ApiException;
import com.coduel.flow.FriendshipFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.UserNotificationPublisher;
import com.coduel.model.data.FriendData;
import com.coduel.model.data.FriendRequestData;
import com.coduel.model.result.FriendDeclineResult;
import com.coduel.model.result.FriendRequestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FriendshipDto {

    @Autowired
    private FriendshipFlow friendshipFlow;
    @Autowired
    private UserNotificationPublisher userNotificationPublisher;

    public void sendRequest(String googleId, Long targetUserId) throws ApiException {
        FriendRequestResult result = friendshipFlow.sendRequest(googleId, targetUserId);
        // Publish post-transaction so the request is committed before the push fires.
        userNotificationPublisher.publish(
                result.getAddressee().getGoogleId(),
                ConversionHelper.toFriendRequestNotification(result.getFriendship(), result.getRequester()));
    }

    public void accept(String googleId, Long requestId) throws ApiException {
        FriendRequestResult result = friendshipFlow.accept(googleId, requestId);
        // Tell the original requester their request was accepted — published post-transaction, with
        // the acceptor as the "from". Declines stay silent (no event), so a rejection drops quietly.
        userNotificationPublisher.publish(
                result.getRequester().getGoogleId(),
                ConversionHelper.toFriendAcceptedNotification(result.getAddressee()));
    }

    public void remove(String googleId, Long requestId) throws ApiException {
        FriendDeclineResult result = friendshipFlow.remove(googleId, requestId);
        // Only a decline of a pending request notifies the requester (post-commit) — and silently, so
        // their "Requested" button reverts to "Add" with no toast. A cancel pings no one.
        if (result.getRequesterToNotify() != null) {
            userNotificationPublisher.publish(
                    result.getRequesterToNotify().getGoogleId(),
                    ConversionHelper.toFriendDeclinedNotification(result.getDecliner()));
        }
    }

    public void unfriend(String googleId, Long otherUserId) throws ApiException {
        friendshipFlow.unfriend(googleId, otherUserId);
    }

    public List<FriendData> listFriends(String googleId) throws ApiException {
        return friendshipFlow.listFriends(googleId).stream().map(ConversionHelper::toFriendData).toList();
    }

    public List<FriendRequestData> listIncoming(String googleId) throws ApiException {
        return friendshipFlow.listIncoming(googleId).stream().map(ConversionHelper::toFriendRequestData).toList();
    }
}
