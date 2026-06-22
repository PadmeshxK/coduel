package com.coduel.websocket;

import com.coduel.api.FriendshipApi;
import com.coduel.api.UserApi;
import com.coduel.dto.RoomDto;
import com.coduel.entity.Friendship;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.TypingPublisher;
import com.coduel.model.constant.FriendshipStatus;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Inbound chat signals over STOMP. For now just typing indicators: a client SENDs the recipient's
 * userId to /app/chat/typing while composing, and we forward an ephemeral "X is typing" to that
 * recipient (friends only, never persisted). A plain-String payload sidesteps the JSON inbound
 * converter; the client throttles sends so this stays cheap.
 */
@Controller
@Log4j2
public class ChatSocketController {

    @Autowired
    private UserApi userApi;
    @Autowired
    private FriendshipApi friendshipApi;
    @Autowired
    private TypingPublisher typingPublisher;
    @Autowired
    private RoomDto roomDto;

    @MessageMapping("/chat/typing")
    public void typing(@Payload String recipientUserIdRaw, Principal principal) {
        if (principal == null || recipientUserIdRaw == null) {
            return;
        }
        try {
            Long senderId = userApi.getCheckByGoogleId(principal.getName()).getId();
            Long recipientId = Long.parseLong(recipientUserIdRaw.trim());
            Friendship friendship = friendshipApi.findBetween(senderId, recipientId);
            if (friendship == null || friendship.getStatus() != FriendshipStatus.ACCEPTED) {
                return; // only friends can see each other typing
            }
            String recipientGoogleId = userApi.getCheckById(recipientId).getGoogleId();
            typingPublisher.publish(recipientGoogleId, ConversionHelper.toTypingData(senderId));
        } catch (Exception e) {
            log.debug("Typing signal ignored: {}", e.getMessage());
        }
    }

    // Lobby chat: a room member SENDs a plain-text line to /app/room/{roomId}/chat; the Dto gates it
    // on membership, records it in the ring buffer, and broadcasts it to the room's chat topic.
    @MessageMapping("/room/{roomId}/chat")
    public void roomChat(@DestinationVariable Long roomId, @Payload String body, Principal principal) {
        if (principal == null || body == null || body.isBlank()) {
            return;
        }
        try {
            roomDto.postChat(principal.getName(), roomId, body);
        } catch (Exception e) {
            log.debug("Room chat ignored: {}", e.getMessage());
        }
    }
}
