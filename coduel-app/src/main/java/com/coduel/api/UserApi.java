package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.UserDao;
import com.coduel.entity.User;
import com.coduel.model.constant.Errors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class UserApi extends AbstractApi {

    @Autowired
    private UserDao userDao;

    public User upsert(User incoming) {
        User existing = userDao.selectByGoogleId(incoming.getGoogleId());
        if (Objects.isNull(existing)) {
            // First login = signup. The Google name is only a provisional default; displayNameSet stays
            // false so the user is routed to setup to choose a unique name.
            return userDao.persist(incoming);
        }
        // Returning login: refresh email only. displayName and avatarUrl are user-owned once chosen —
        // re-applying the Google values here would clobber a name the user deliberately set.
        if (Objects.nonNull(incoming.getEmail())) {
            existing.setEmail(incoming.getEmail());
        }
        return existing;
    }

    public List<User> searchByDisplayNamePrefix(String prefix, int limit) {
        return userDao.selectByDisplayNamePrefix(prefix, limit);
    }

    // True if another user (not excludeUserId) already has this exact display name. Comparison is
    // case-SENSITIVE ("John" and "john" are distinct): the query narrows candidates case-insensitively
    // (DB-collation agnostic), the exact match is enforced in Java so it holds regardless of collation.
    public boolean isDisplayNameTaken(String displayName, Long excludeUserId) {
        return userDao.selectByDisplayName(displayName).stream()
                .filter(user -> displayName.equals(user.getDisplayName()))
                .anyMatch(user -> !user.getId().equals(excludeUserId));
    }

    // Batch-load users by id (empty in -> empty out) — for decorating lists without an N+1.
    public List<User> getByIds(List<Long> ids) {
        return userDao.selectByIds(ids);
    }

    public User getCheckById(Long id) throws ApiException {
        User user = userDao.selectById(id);
        if (Objects.isNull(user)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_108, List.of(id));
        }
        return user;
    }

    public User getCheckByGoogleId(String googleId) throws ApiException {
        User user = userDao.selectByGoogleId(googleId);
        if (Objects.isNull(user)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_109, List.of());
        }
        return user;
    }

    // Profile edit: the entity is managed within this @Transactional method, so the change is
    // dirty-checked and flushed on commit (no explicit persist needed).
    public User updateProfile(String googleId, String displayName, String avatarUrl) throws ApiException {
        User user = getCheckByGoogleId(googleId);
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        // Saving a profile is an explicit name choice — mark it claimed so it's enforced as unique.
        user.setDisplayNameSet(true);
        return user;
    }
}
