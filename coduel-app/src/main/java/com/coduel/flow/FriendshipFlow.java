package com.coduel.flow;

import com.coduel.api.FriendshipApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Friendship;
import com.coduel.entity.User;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.FriendshipStatus;
import com.coduel.model.result.FriendDeclineResult;
import com.coduel.model.result.FriendListResult;
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
public class FriendshipFlow {

    @Autowired
    private FriendshipApi friendshipApi;
    @Autowired
    private UserApi userApi;

    public FriendRequestResult sendRequest(String googleId, Long targetUserId) throws ApiException {
        User requester = userApi.getCheckByGoogleId(googleId);
        User addressee = userApi.getCheckById(targetUserId); // 404 if the target doesn't exist
        if (requester.getId().equals(targetUserId)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_114, List.of());
        }
        Friendship request = ConversionHelper.convert(requester.getId(), addressee.getId());
        friendshipApi.add(request);
        return new FriendRequestResult(requester, addressee, request);
    }

    // Returns both parties so the Dto can push a "you're now friends" notification to the original
    // requester after commit (requester = who to notify, addressee = the acceptor / the "from").
    public FriendRequestResult accept(String googleId, Long requestId) throws ApiException {
        User acceptor = userApi.getCheckByGoogleId(googleId);
        Friendship request = friendshipApi.acceptRequest(acceptor.getId(), requestId);
        User requester = userApi.getCheckById(request.getRequesterId());
        return new FriendRequestResult(requester, acceptor, request);
    }

    // Decline an incoming request, or cancel one you sent — either party may drop the row. Returns
    // whom (if anyone) to notify: only a decline (the addressee dropping a still-pending request)
    // pings the original requester so their "Requested" button reverts; a cancel notifies no one.
    public FriendDeclineResult remove(String googleId, Long requestId) throws ApiException {
        User actor = userApi.getCheckByGoogleId(googleId);
        Friendship friendship = friendshipApi.getCheckById(requestId);
        if (!friendship.getRequesterId().equals(actor.getId())
                && !friendship.getAddresseeId().equals(actor.getId())) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_113, List.of(requestId));
        }
        boolean isDecline = friendship.getStatus() == FriendshipStatus.PENDING
                && friendship.getAddresseeId().equals(actor.getId());
        User requesterToNotify = isDecline ? userApi.getCheckById(friendship.getRequesterId()) : null;
        friendshipApi.delete(friendship);
        return new FriendDeclineResult(requesterToNotify, actor);
    }

    public void unfriend(String googleId, Long otherUserId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Friendship friendship = friendshipApi.findBetween(me, otherUserId);
        if (Objects.isNull(friendship)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_113, List.of(otherUserId));
        }
        friendshipApi.delete(friendship);
    }

    public List<FriendListResult> listFriends(String googleId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        List<FriendListResult> friends = new ArrayList<>();
        for (Friendship friendship : friendshipApi.getAccepted(me)) {
            Long otherId = friendship.getRequesterId().equals(me)
                    ? friendship.getAddresseeId()
                    : friendship.getRequesterId();
            // created_at = when the friendship row was made; stable "friends since" basis.
            Long sinceMs = friendship.getCreatedAt() != null ? friendship.getCreatedAt().toEpochMilli() : null;
            friends.add(ConversionHelper.toFriendListResult(userApi.getCheckById(otherId), sinceMs));
        }
        return friends;
    }

    public List<IncomingFriendRequestResult> listIncoming(String googleId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        List<IncomingFriendRequestResult> requests = new ArrayList<>();
        for (Friendship friendship : friendshipApi.getIncoming(me)) {
            requests.add(new IncomingFriendRequestResult(friendship, userApi.getCheckById(friendship.getRequesterId())));
        }
        return requests;
    }
}
