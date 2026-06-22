package com.coduel.dao;

import com.coduel.entity.Room;
import org.springframework.stereotype.Repository;

@Repository
public class RoomDao extends BaseDao<Room> {

    public RoomDao() {
        super(Room.class);
    }
}
