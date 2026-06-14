package com.coduel.dao;

import com.coduel.entity.Leaderboard;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LeaderboardDao extends BaseDao<Leaderboard> {

    private static final String SELECT_BY_USER = "SELECT l FROM Leaderboard l WHERE l.userId = :userId";
    // most wins first; fewer losses breaks ties.
    private static final String SELECT_TOP = "SELECT l FROM Leaderboard l ORDER BY l.wins DESC, l.losses ASC";

    public LeaderboardDao() {
        super(Leaderboard.class);
    }

    public Leaderboard selectByUserId(Long userId) {
        return selectSingleOrNull(createQuery(SELECT_BY_USER, Leaderboard.class).setParameter("userId", userId));
    }

    public List<Leaderboard> selectPageByWins(int page, int size) {
        return createQuery(SELECT_TOP, Leaderboard.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }
}
