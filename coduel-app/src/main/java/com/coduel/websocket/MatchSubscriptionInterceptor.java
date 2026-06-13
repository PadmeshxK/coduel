package com.coduel.websocket;

import com.coduel.api.MatchParticipantApi;
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
 * Bouncer for match topics: only a match's participants may SUBSCRIBE to /topic/match/{id}.
 * Runs on every inbound STOMP frame; lets non-SUBSCRIBE frames and non-match topics through untouched.
 */
@Component
public class MatchSubscriptionInterceptor implements ChannelInterceptor {

    private static final String MATCH_TOPIC_PREFIX = "/topic/match/";

    @Autowired
    private UserApi userApi;
    @Autowired
    private MatchParticipantApi matchParticipantApi;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(MATCH_TOPIC_PREFIX)) {
            return message;
        }

        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new MessagingException("Not authenticated");
        }
        long matchId = parseMatchId(destination);
        try {
            Long userId = userApi.getCheckByGoogleId(principal.getName()).getId();
            boolean participant = matchParticipantApi.getByMatchId(matchId).stream()
                    .anyMatch(p -> p.getUserId().equals(userId));
            if (!participant) {
                throw new MessagingException("Not a participant of match " + matchId);
            }
        } catch (ApiException e) {
            throw new MessagingException("Subscription denied", e);
        }
        return message;
    }

    private long parseMatchId(String destination) {
        try {
            return Long.parseLong(destination.substring(MATCH_TOPIC_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new MessagingException("Invalid match topic: " + destination);
        }
    }
}
