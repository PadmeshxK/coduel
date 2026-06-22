package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.RoomDao;
import com.coduel.entity.Room;
import com.coduel.model.constant.Errors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class RoomApi extends AbstractApi {

    @Autowired
    private RoomDao roomDao;

    public Room add(Room room) {
        return roomDao.persist(room);
    }

    public Room getCheckById(Long id) throws ApiException {
        Room room = roomDao.selectById(id);
        if (Objects.isNull(room)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_120, List.of(id));
        }
        return room;
    }
}
