package com.coduel.model.constant;

public enum RoomEventType {

    // A member joined or left — re-fetch the roster.
    ROSTER_CHANGED,
    // The host started a match — clients jump into the arena (matchId carried on the event).
    MATCH_STARTED,
    // The host closed the room — members return home.
    ROOM_CLOSED
}
