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
            return userDao.persist(incoming);
        }
        // Only overwrite a field when the incoming value is present, so a null never wipes an existing value.
        if (Objects.nonNull(incoming.getEmail())) {
            existing.setEmail(incoming.getEmail());
        }
        if (Objects.nonNull(incoming.getDisplayName())) {
            existing.setDisplayName(incoming.getDisplayName());
        }
        if (Objects.nonNull(incoming.getAvatarUrl())) {
            existing.setAvatarUrl(incoming.getAvatarUrl());
        }
        return existing;
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
}
