package com.coduel.model.result;

import com.coduel.entity.Room;
import com.coduel.entity.RoomMember;
import com.coduel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

// Internal carrier for rendering a room: the room + its roster (members carry readiness, with
// profiles looked up by userId), who the host is, who's asking, and the in-progress match id.
@Getter
@AllArgsConstructor
public class RoomDetailResult {

    private Room room;
    private List<RoomMember> members;
    private Map<Long, User> profiles;
    private Long hostId;
    private Long requestingUserId;
    private Long activeMatchId;
}
