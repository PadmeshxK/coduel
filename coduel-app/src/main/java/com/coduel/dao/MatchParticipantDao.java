package com.coduel.dao;

import com.coduel.entity.MatchParticipant;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MatchParticipantDao extends BaseDao<MatchParticipant> {

    private static final String SELECT_BY_MATCH =
            "SELECT p FROM MatchParticipant p WHERE p.matchId = :matchId ORDER BY p.id";
    private static final String SELECT_BY_USER =
            "SELECT p FROM MatchParticipant p WHERE p.userId = :userId ORDER BY p.id DESC";

    public MatchParticipantDao() {
        super(MatchParticipant.class);
    }

    public List<MatchParticipant> selectByMatchId(Long matchId) {
        return createQuery(SELECT_BY_MATCH, MatchParticipant.class)
                .setParameter("matchId", matchId)
                .getResultList();
    }

    // most-recent first, so the caller can find the user's current match quickly.
    public List<MatchParticipant> selectByUserId(Long userId) {
        return createQuery(SELECT_BY_USER, MatchParticipant.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
