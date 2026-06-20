package com.coduel.flow;

import com.coduel.api.FriendApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Friendship;
import com.coduel.entity.User;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.FriendshipStatus;
import com.coduel.model.result.FriendRequestResult;
import com.coduel.model.result.IncomingFriendRequestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Transactional(rollbackFor = ApiException.class)
public class FriendFlow {

    @Autowired
    private FriendApi friendApi;
    @Autowired
    private UserApi userApi;

    public FriendRequestResult sendRequest(String googleId, Long targetUserId) throws ApiException {
        User requester = userApi.getCheckByGoogleId(googleId);
        User addressee = userApi.getCheckById(targetUserId); // 404 if the target doesn't exist
        if (requester.getId().equals(targetUserId)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_114, List.of());
        }
        if (Objects.nonNull(friendApi.findBetween(requester.getId(), targetUserId))) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_115, List.of(targetUserId));
        }
        Friendship friendship = new Friendship();
        friendship.setRequesterId(requester.getId());
        friendship.setAddresseeId(targetUserId);
        friendship.setStatus(FriendshipStatus.PENDING);
        return new FriendRequestResult(requester, addressee, friendApi.add(friendship));
    }

    public void accept(String googleId, Long requestId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Friendship request = friendApi.getCheckById(requestId);
        // Only the addressee of a still-pending request may accept it.
        if (!request.getAddresseeId().equals(me) || request.getStatus() != FriendshipStatus.PENDING) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_113, List.of(requestId));
        }
        request.setStatus(FriendshipStatus.ACCEPTED);
    }

    // Decline an incoming request, or cancel one you sent — either party may drop the row.
    public void remove(String googleId, Long requestId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Friendship friendship = friendApi.getCheckById(requestId);
        if (!friendship.getRequesterId().equals(me) && !friendship.getAddresseeId().equals(me)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_113, List.of(requestId));
        }
        friendApi.delete(friendship);
    }

    public void unfriend(String googleId, Long otherUserId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Friendship friendship = friendApi.findBetween(me, otherUserId);
        if (Objects.isNull(friendship)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_113, List.of(otherUserId));
        }
        friendApi.delete(friendship);
    }

    public List<User> listFriends(String googleId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        List<User> friends = new ArrayList<>();
        for (Friendship friendship : friendApi.getAccepted(me)) {
            Long otherId = friendship.getRequesterId().equals(me)
                    ? friendship.getAddresseeId()
                    : friendship.getRequesterId();
            friends.add(userApi.getCheckById(otherId));
        }
        return friends;
    }

    public List<IncomingFriendRequestResult> listIncoming(String googleId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        List<IncomingFriendRequestResult> requests = new ArrayList<>();
        for (Friendship friendship : friendApi.getIncoming(me)) {
            requests.add(new IncomingFriendRequestResult(friendship, userApi.getCheckById(friendship.getRequesterId())));
        }
        return requests;
    }
}
