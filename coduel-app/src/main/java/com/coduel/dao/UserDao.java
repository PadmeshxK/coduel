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
    // Any user whose display name matches (case-insensitively) — candidates for a uniqueness check.
    // Not gated on displayNameSet: a name in the table is taken regardless of whether that user has
    // formally been through setup (existing accounts default displayNameSet=false). NULL names (brand
    // new accounts, pre-setup) never match the equality, so they don't block anyone.
    private static final String SELECT_BY_DISPLAY_NAME =
            "SELECT u FROM User u WHERE LOWER(u.displayName) = LOWER(:name)";
    // Batch-fetch by id — for decorating a list (e.g. the inbox's "other user") without an N+1.
    private static final String SELECT_BY_IDS = "SELECT u FROM User u WHERE u.id IN :ids";

    public UserDao() {
        super(User.class);
    }

    public List<User> selectByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return createQuery(SELECT_BY_IDS, User.class).setParameter("ids", ids).getResultList();
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

    public List<User> selectByDisplayName(String name) {
        return createQuery(SELECT_BY_DISPLAY_NAME, User.class)
                .setParameter("name", name)
                .getResultList();
    }
}
