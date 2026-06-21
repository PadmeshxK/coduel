package com.coduel.flow;

import com.coduel.api.RoomApi;
import com.coduel.common.exception.ApiException;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.NotificationEventType;
import com.coduel.model.constant.RoomState;
import com.coduel.model.data.NotificationData;
import com.coduel.model.result.IncomingFriendRequestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregates everything that should surface in the user's notification center: pending friend
 * requests (persisted in the DB) and the inbox notifications (room invites, duel challenges — read
 * from Redis by the Dto and passed in). Sorted most-recent-first.
 */
@Component
@Transactional(rollbackFor = ApiException.class)
public class NotificationFlow {

    @Autowired
    private FriendshipFlow friendshipFlow;
    @Autowired
    private RoomApi roomApi;

    // The Dto loads the inbox (Redis) and hands it in; the flow only does the DB-backed work —
    // resolving friend requests and dropping invites whose room is no longer joinable.
    public List<NotificationData> getPendingNotifications(String googleId, List<NotificationData> inbox)
            throws ApiException {
        List<NotificationData> pending = new ArrayList<>();

        for (IncomingFriendRequestResult view : friendshipFlow.listIncoming(googleId)) {
            pending.add(ConversionHelper.toFriendRequestNotification(view.getFriendship(), view.getRequester()));
        }
        for (NotificationData notification : inbox) {
            // A room invite is only worth showing while its room is still open to join; everything
            // else in the inbox (e.g. a duel challenge) stands until it expires or is acted on.
            if (notification.getType() == NotificationEventType.ROOM_INVITE
                    && !isOpenRoom(notification.getRoomId())) {
                continue;
            }
            pending.add(notification);
        }

        // Most recent first; missing timestamps sink to the bottom.
        pending.sort(Comparator.comparing(NotificationData::getCreatedAtMs,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return pending;
    }

    private boolean isOpenRoom(Long roomId) {
        try {
            return roomApi.getCheckById(roomId).getState() == RoomState.OPEN;
        } catch (ApiException e) {
            return false; // room gone -> stale invite, skip (inbox TTL will reap it)
        }
    }
}
