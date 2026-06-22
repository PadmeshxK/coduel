package com.coduel.dto;

import com.coduel.common.exception.ApiException;
import com.coduel.entity.Room;
import com.coduel.flow.RoomFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.NotificationInbox;
import com.coduel.interfaces.RoomEventPublisher;
import com.coduel.interfaces.UserNotificationPublisher;
import com.coduel.model.data.NotificationData;
import com.coduel.model.data.RoomData;
import com.coduel.model.result.InviteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoomDto {

    @Autowired
    private RoomFlow roomFlow;
    @Autowired
    private RoomEventPublisher roomEventPublisher;
    @Autowired
    private UserNotificationPublisher userNotificationPublisher;
    @Autowired
    private NotificationInbox notificationInbox;

    public RoomData create(String googleId) throws ApiException {
        Room room = roomFlow.create(googleId);
        return ConversionHelper.toRoomData(roomFlow.getView(room.getId(), googleId));
    }

    public RoomData get(Long roomId, String googleId) throws ApiException {
        return ConversionHelper.toRoomData(roomFlow.getView(roomId, googleId));
    }

    public void invite(Long roomId, Long inviteeId, String googleId) throws ApiException {
        InviteResult result = roomFlow.invite(roomId, inviteeId, googleId);
        NotificationData notification = ConversionHelper.toRoomInviteNotification(roomId, result.getFromUser());
        String inviteeGoogleId = result.getInvitee().getGoogleId();
        // Live push for online invitees + persist so it survives a reload / offline gap.
        userNotificationPublisher.publish(inviteeGoogleId, notification);
        notificationInbox.add(inviteeGoogleId, notification);
    }

    public void join(Long roomId, String googleId) throws ApiException {
        roomFlow.join(roomId, googleId);
        // The invite is consumed — drop it so it stops showing in the joiner's notifications.
        notificationInbox.remove(googleId, ConversionHelper.roomNotificationId(roomId));
        roomEventPublisher.publish(roomId, ConversionHelper.toRoomRosterChanged());
    }

    public RoomData start(String googleId, Long roomId) throws ApiException {
        Long matchId = roomFlow.start(roomId, googleId);
        // Tell everyone in the lobby to jump into the new match.
        roomEventPublisher.publish(roomId, ConversionHelper.toRoomMatchStarted(matchId));
        return ConversionHelper.toRoomData(roomFlow.getView(roomId, googleId));
    }

    public RoomData setReady(String googleId, Long roomId, boolean ready) throws ApiException {
        roomFlow.setReady(roomId, googleId, ready);
        // Broadcast so every lobby client re-fetches and sees the updated ready state.
        roomEventPublisher.publish(roomId, ConversionHelper.toRoomRosterChanged());
        return ConversionHelper.toRoomData(roomFlow.getView(roomId, googleId));
    }

    public void leave(Long roomId, String googleId) throws ApiException {
        boolean closed = roomFlow.leave(roomId, googleId);
        roomEventPublisher.publish(roomId,
                closed ? ConversionHelper.toRoomClosed() : ConversionHelper.toRoomRosterChanged());
    }
}
