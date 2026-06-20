package com.coduel.model.data;

import com.coduel.model.constant.RoomState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RoomData {

    private Long roomId;
    private RoomState state;
    private boolean host;
    private int maxPlayers;
    private List<RoomParticipantData> participants;
    // The in-progress match to jump into, or null when the room is idle (lobby).
    private Long activeMatchId;
}
