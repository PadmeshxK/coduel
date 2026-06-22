package com.coduel.interfaces;

import com.coduel.model.data.RoomChatData;

/** Port for broadcasting a lobby-chat message to everyone subscribed to that room's chat topic. */
public interface RoomChatPublisher {

    void publish(Long roomId, RoomChatData message);
}
