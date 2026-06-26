package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Room;
import com.coduel.interfaces.RoomRegistry;
import com.coduel.model.constant.Errors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class RoomApi extends AbstractApi {

    @Autowired
    private RoomRegistry roomRegistry;

    // Persist the aggregate (built by the caller via ConversionHelper). Assigns an id on first save —
    // the Redis analogue of an IDENTITY column — then writes it through, mirroring dao.persist.
    public Room save(Room room) {
        if (Objects.isNull(room.getId())) {
            room.setId(roomRegistry.nextId());
        }
        roomRegistry.save(room);
        return room;
    }

    public Room getCheckById(Long id) throws ApiException {
        Room room = roomRegistry.get(id);
        if (Objects.isNull(room)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_120, List.of(id));
        }
        return room;
    }

    /** Nullable lookup for callers that must tolerate a missing/expired room (presence, the interceptor). */
    public Room findById(Long id) {
        return roomRegistry.get(id);
    }

    public void delete(Long id) {
        roomRegistry.delete(id);
    }
}
