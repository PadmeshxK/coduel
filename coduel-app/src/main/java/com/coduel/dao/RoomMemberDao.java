package com.coduel.dao;

import com.coduel.entity.RoomMember;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RoomMemberDao extends BaseDao<RoomMember> {

    // Ordered by id so the oldest member (the host) is always first.
    private static final String SELECT_BY_ROOM =
            "SELECT m FROM RoomMember m WHERE m.roomId = :roomId ORDER BY m.id";

    public RoomMemberDao() {
        super(RoomMember.class);
    }

    public List<RoomMember> selectByRoomId(Long roomId) {
        return createQuery(SELECT_BY_ROOM, RoomMember.class)
                .setParameter("roomId", roomId)
                .getResultList();
    }
}
