package com.coduel.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * One person in a room's roster, embedded in the {@link Room} aggregate. Insertion order is join
 * order, and the first member is the host. Plain POJO (no JPA) — serialized as part of the room JSON.
 */
@Getter
@Setter
public class RoomMember {

    private Long userId;

    // Lobby readiness. The host is implicitly ready (starting is their signal), so this only applies
    // to non-host members; the host can't start until everyone else is ready.
    private boolean ready;
}
