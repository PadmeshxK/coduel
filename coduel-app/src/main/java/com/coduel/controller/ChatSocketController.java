package com.coduel.controller;

import com.coduel.common.exception.ApiException;
import com.coduel.dto.ChatDto;
import com.coduel.dto.RoomDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Inbound chat signals over STOMP — the WebSocket counterpart of the REST ChatController (the socket is
 * the right fit here: high-frequency, fire-and-forget). Lives in the controller package and follows the
 * same controller -> dto -> flow hierarchy: just delegate; parsing/validation/gating live in the dto +
 * flow. Destinations sit under /chat to mirror the REST namespace — /app/chat/dm/typing and
 * /app/chat/room/{roomId} — instead of the earlier inconsistent /app/chat/typing + /app/room/{id}/chat.
 */
@Controller
@MessageMapping("/chat")
@Log4j2
public class ChatSocketController {

    @Autowired
    private ChatDto chatDto;
    @Autowired
    private RoomDto roomDto;

    // DM typing: client SENDs the recipient's userId while composing → forwarded live (friends only).
    @MessageMapping("/dm/typing")
    public void typing(@Payload String recipientUserId, Principal principal) throws ApiException {
        chatDto.sendTyping(principal.getName(), recipientUserId);
    }

    // Lobby/room chat: a member SENDs a line → buffered in Redis + broadcast to the room.
    @MessageMapping("/room/{roomId}")
    public void roomChat(@DestinationVariable Long roomId, @Payload String body, Principal principal) throws ApiException {
        roomDto.postChat(principal.getName(), roomId, body);
    }

    // Best-effort fire-and-forget signals — never push an error frame onto the fragile shared socket.
    @MessageExceptionHandler(Exception.class)
    public void onError(Exception e) {
        log.debug("Chat socket signal dropped: {}", e.getMessage());
    }
}
