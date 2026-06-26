package com.coduel.entity;

import com.coduel.model.constant.RoomState;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * An ephemeral private lobby — kept in Redis with a TTL, NOT the relational DB (it's transient
 * coordination state, not durable history). It still outlives any single match (start → play → return
 * → rematch), but a closed/abandoned room simply expires. The roster is embedded; the first member
 * (insertion order) is the host, so host transfer on leave is implicit. The in-progress match (if any)
 * is the Match row whose roomId points here and whose state is ACTIVE.
 *
 * Plain POJO (no JPA) — serialized to/from JSON by the room registry.
 */
@Getter
@Setter
public class Room {

    private Long id;
    private RoomState state;
    private Long createdAtMs;
    private List<RoomMember> members = new ArrayList<>();
}
