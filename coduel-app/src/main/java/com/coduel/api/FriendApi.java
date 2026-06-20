package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.FriendshipDao;
import com.coduel.entity.Friendship;
import com.coduel.model.constant.Errors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class FriendApi extends AbstractApi {

    @Autowired
    private FriendshipDao friendshipDao;

    public Friendship add(Friendship friendship) {
        return friendshipDao.persist(friendship);
    }

    public Friendship getCheckById(Long id) throws ApiException {
        Friendship friendship = friendshipDao.selectById(id);
        if (Objects.isNull(friendship)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_113, List.of(id));
        }
        return friendship;
    }

    public Friendship findBetween(Long a, Long b) {
        return friendshipDao.selectBetween(a, b);
    }

    public List<Friendship> getAccepted(Long userId) {
        return friendshipDao.selectAccepted(userId);
    }

    public List<Friendship> getIncoming(Long userId) {
        return friendshipDao.selectIncoming(userId);
    }

    public void delete(Friendship friendship) {
        friendshipDao.delete(friendship);
    }
}
