package com.coduel.websocket;

import com.coduel.api.MatchParticipantApi;
import com.coduel.api.RoomMemberApi;
import com.coduel.api.UserApi;
import com.coduel.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Bouncer for the live topics: only a match's participants may SUBSCRIBE to /topic/match/{id}, and
 * only a room's members may SUBSCRIBE to /topic/room/{id}. Runs on every inbound STOMP frame; lets
 * non-SUBSCRIBE frames and other destinations through untouched.
 */
@Component
public class MatchSubscriptionInterceptor implements ChannelInterceptor {

    private static final String MATCH_TOPIC_PREFIX = "/topic/match/";
    private static final String ROOM_TOPIC_PREFIX = "/topic/room/";

    @Autowired
    private UserApi userApi;
    @Autowired
    private MatchParticipantApi matchParticipantApi;
    @Autowired
    private RoomMemberApi roomMemberApi;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        try {
            if (destination.startsWith(MATCH_TOPIC_PREFIX)) {
                long matchId = parseId(destination, MATCH_TOPIC_PREFIX);
                long userId = requireUserId(accessor);
                boolean participant = matchParticipantApi.getByMatchId(matchId).stream()
                        .anyMatch(p -> p.getUserId().equals(userId));
                if (!participant) {
                    throw new MessagingException("Not a participant of match " + matchId);
                }
            } else if (destination.startsWith(ROOM_TOPIC_PREFIX)) {
                long roomId = parseId(destination, ROOM_TOPIC_PREFIX);
                long userId = requireUserId(accessor);
                boolean member = roomMemberApi.getByRoomId(roomId).stream()
                        .anyMatch(m -> m.getUserId().equals(userId));
                if (!member) {
                    throw new MessagingException("Not a member of room " + roomId);
                }
            }
        } catch (ApiException e) {
            throw new MessagingException("Subscription denied", e);
        }
        return message;
    }

    private long requireUserId(StompHeaderAccessor accessor) throws ApiException {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new MessagingException("Not authenticated");
        }
        return userApi.getCheckByGoogleId(principal.getName()).getId();
    }

    private long parseId(String destination, String prefix) {
        try {
            return Long.parseLong(destination.substring(prefix.length()));
        } catch (NumberFormatException e) {
            throw new MessagingException("Invalid topic: " + destination);
        }
    }
}
