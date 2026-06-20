package com.coduel.model.data;

import com.coduel.model.constant.RoomEventType;
import lombok.Getter;
import lombok.Setter;

// Payload pushed to /topic/room/{roomId} (the persistent lobby channel).
@Getter
@Setter
public class RoomEventData {

    private RoomEventType type;
    // MATCH_STARTED — the match to jump into.
    private Long matchId;
}
