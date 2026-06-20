package com.coduel.dao;

import com.coduel.entity.Friendship;
import com.coduel.model.constant.FriendshipStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FriendshipDao extends BaseDao<Friendship> {

    // The single row for a pair, in either direction (used to block duplicate / reverse requests).
    private static final String SELECT_BETWEEN =
            "SELECT f FROM Friendship f WHERE (f.requesterId = :a AND f.addresseeId = :b) "
                    + "OR (f.requesterId = :b AND f.addresseeId = :a)";
    private static final String SELECT_ACCEPTED =
            "SELECT f FROM Friendship f WHERE f.status = :accepted "
                    + "AND (f.requesterId = :userId OR f.addresseeId = :userId) ORDER BY f.updatedAt DESC";
    private static final String SELECT_INCOMING =
            "SELECT f FROM Friendship f WHERE f.status = :pending AND f.addresseeId = :userId ORDER BY f.createdAt DESC";

    public FriendshipDao() {
        super(Friendship.class);
    }

    public Friendship selectBetween(Long a, Long b) {
        return selectSingleOrNull(createQuery(SELECT_BETWEEN, Friendship.class)
                .setParameter("a", a)
                .setParameter("b", b));
    }

    public List<Friendship> selectAccepted(Long userId) {
        return createQuery(SELECT_ACCEPTED, Friendship.class)
                .setParameter("accepted", FriendshipStatus.ACCEPTED)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<Friendship> selectIncoming(Long userId) {
        return createQuery(SELECT_INCOMING, Friendship.class)
                .setParameter("pending", FriendshipStatus.PENDING)
                .setParameter("userId", userId)
                .getResultList();
    }
}
