package com.coduel.flow;

import com.coduel.api.FriendshipApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Friendship;
import com.coduel.entity.User;
import com.coduel.model.constant.Errors;
import com.coduel.model.result.FriendResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = ApiException.class)
public class UserFlow {

    private static final int SEARCH_LIMIT = 20;

    @Autowired
    private UserApi userApi;
    @Autowired
    private FriendshipApi friendshipApi;

    public User getByGoogleId(String googleId) throws ApiException {
        return userApi.getCheckByGoogleId(googleId);
    }

    // Find people to add as friends by display-name prefix, each tagged with the caller's relationship
    // (already friends / pending request) so the UI shows the right action and "Requested" survives a
    // reload (it's backend truth, not session-local state).
    public List<FriendResult> searchByUserPrefix(String query, String googleId) throws ApiException {
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        Set<Long> pending = friendshipApi.getOutgoing(me).stream()
                .map(Friendship::getAddresseeId)
                .collect(Collectors.toSet());
        Set<Long> friends = friendshipApi.getAccepted(me).stream()
                .map(f -> f.getRequesterId().equals(me) ? f.getAddresseeId() : f.getRequesterId())
                .collect(Collectors.toSet());

        return userApi.searchByDisplayNamePrefix(query, SEARCH_LIMIT).stream()
                .map(user -> new FriendResult(user,
                        friends.contains(user.getId()),
                        pending.contains(user.getId())))
                .toList();
    }

    public User updateProfile(String googleId, String displayName, String avatarUrl) throws ApiException {
        User me = userApi.getCheckByGoogleId(googleId);
        if (userApi.isDisplayNameTaken(displayName, me.getId())) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_126, List.of());
        }
        return userApi.updateProfile(googleId, displayName, avatarUrl);
    }

    // Live availability for the setup/edit UI. Blank is never "available"; the caller's own current
    // name counts as available (editing your profile without changing the name shouldn't flag a clash).
    public boolean isDisplayNameAvailable(String displayName, String googleId) throws ApiException {
        if (displayName == null || displayName.isBlank()) {
            return false;
        }
        Long me = userApi.getCheckByGoogleId(googleId).getId();
        return !userApi.isDisplayNameTaken(displayName.trim(), me);
    }
}
