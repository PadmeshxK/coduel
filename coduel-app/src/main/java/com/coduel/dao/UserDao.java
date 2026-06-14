package com.coduel.dao;

import com.coduel.entity.User;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDao extends BaseDao<User> {

    private static final String SELECT_BY_GOOGLE_ID = "SELECT u FROM User u WHERE u.googleId = :googleId";
    // Case-insensitive prefix match; index-friendly (caller passes "prefix%").
    private static final String SELECT_BY_NAME_PREFIX =
            "SELECT u FROM User u WHERE LOWER(u.displayName) LIKE :prefix ORDER BY u.displayName ASC";

    public UserDao() {
        super(User.class);
    }

    public User selectByGoogleId(String googleId) {
        return selectSingleOrNull(createQuery(SELECT_BY_GOOGLE_ID, User.class).setParameter("googleId", googleId));
    }

    public List<User> selectByDisplayNamePrefix(String prefix, int limit) {
        return createQuery(SELECT_BY_NAME_PREFIX, User.class)
                .setParameter("prefix", prefix.toLowerCase() + "%")
                .setMaxResults(limit)
                .getResultList();
    }
}
