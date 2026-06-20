package com.coduel.flow;

import com.coduel.api.RoomApi;
import com.coduel.common.exception.ApiException;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.MatchInvitation;
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
 * requests (persisted in the DB) and live room invites (in Redis, kept only while the room is still
 * OPEN). Sorted most-recent-first.
 */
@Component
@Transactional(rollbackFor = ApiException.class)
public class NotificationFlow {

    @Autowired
    private FriendFlow friendFlow;
    @Autowired
    private MatchInvitation matchInvitation;
    @Autowired
    private RoomApi roomApi;

    public List<NotificationData> getPendingNotifications(String googleId) throws ApiException {
        List<NotificationData> pending = new ArrayList<>();

        for (IncomingFriendRequestResult view : friendFlow.listIncoming(googleId)) {
            pending.add(ConversionHelper.toFriendRequestNotification(view.getFriendship(), view.getRequester()));
        }
        for (NotificationData invite : matchInvitation.getInvites(googleId)) {
            if (isOpenRoom(invite.getRoomId())) {
                pending.add(invite);
            }
        }

        // Most recent first; missing timestamps sink to the bottom.
        pending.sort(Comparator.comparing(NotificationData::getCreatedAtMs,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return pending;
    }

    // A persisted invite is only worth showing while its room is still open to join.
    private boolean isOpenRoom(Long roomId) {
        try {
            return roomApi.getCheckById(roomId).getState() == RoomState.OPEN;
        } catch (ApiException e) {
            return false; // room gone -> stale invite, skip (Redis TTL will reap it)
        }
    }
}
