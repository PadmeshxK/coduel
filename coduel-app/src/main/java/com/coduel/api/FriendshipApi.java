package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.FriendshipDao;
import com.coduel.entity.Friendship;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.FriendshipStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class FriendshipApi extends AbstractApi {

    @Autowired
    private FriendshipDao friendshipDao;

    public Friendship add(Friendship friendship) throws ApiException {
        Friendship existing = findBetween(friendship.getRequesterId(), friendship.getAddresseeId());
        if (!Objects.isNull(existing)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_115, List.of(friendship.getAddresseeId()));
        }

        return friendshipDao.persist(friendship);
    }

    public Friendship acceptRequest(Long acceptorUserId, Long requestId) throws ApiException {
        Friendship request = getCheckById(requestId);

        if(!request.getAddresseeId().equals(acceptorUserId)){
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_130, List.of(requestId));
        }
        if(!request.getStatus().equals(FriendshipStatus.PENDING)){
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_131, List.of(requestId));
        }

        request.setStatus(FriendshipStatus.ACCEPTED);
        return request;
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

    public List<Friendship> getOutgoing(Long userId) {
        return friendshipDao.selectOutgoing(userId);
    }

    public void delete(Friendship friendship) {
        friendshipDao.delete(friendship);
    }
}
