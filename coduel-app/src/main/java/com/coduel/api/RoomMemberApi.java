package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.RoomMemberDao;
import com.coduel.entity.RoomMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = ApiException.class)
public class RoomMemberApi extends AbstractApi {

    @Autowired
    private RoomMemberDao roomMemberDao;

    public RoomMember add(RoomMember member) {
        return roomMemberDao.persist(member);
    }

    public List<RoomMember> getByRoomId(Long roomId) {
        return roomMemberDao.selectByRoomId(roomId);
    }

    public void delete(RoomMember member) {
        roomMemberDao.delete(member);
    }
}
