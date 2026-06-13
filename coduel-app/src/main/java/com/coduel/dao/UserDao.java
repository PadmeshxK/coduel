package com.coduel.dao;

import com.coduel.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao extends BaseDao<User> {

    private static final String SELECT_BY_GOOGLE_ID = "SELECT u FROM Users u WHERE u.googleId = :googleId";

    public UserDao() {
        super(User.class);
    }

    public User selectByGoogleId(String googleId) {
        return selectSingleOrNull(createQuery(SELECT_BY_GOOGLE_ID, User.class).setParameter("googleId", googleId));
    }
}
