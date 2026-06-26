package com.coduel.interfaces;

import com.coduel.entity.Room;

/** Port for the ephemeral room aggregate. Rooms are transient coordination state, so they carry a TTL. */
public interface RoomRegistry {

    /** A fresh, monotonic room id. */
    Long nextId();

    /** The room, or null if it never existed / has expired. */
    Room get(Long id);

    /** Persist the aggregate (refreshing its TTL). */
    void save(Room room);

    void delete(Long id);
}
