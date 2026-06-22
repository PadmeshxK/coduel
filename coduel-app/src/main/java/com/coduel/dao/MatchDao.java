package com.coduel.dao;

import com.coduel.entity.Match;
import com.coduel.model.constant.MatchState;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class MatchDao extends BaseDao<Match> {

    private static final String SELECT_ACTIVE_OLDER =
            "SELECT m FROM Match m WHERE m.state = :state AND m.createdAt < :cutoff";
    // Most-recent ACTIVE match for a room (the in-progress game), or null if the room is idle.
    private static final String SELECT_ACTIVE_BY_ROOM =
            "SELECT m FROM Match m WHERE m.roomId = :roomId AND m.state = :state ORDER BY m.id DESC";

    public MatchDao() {
        super(Match.class);
    }

    public List<Match> selectActiveOlderThan(Instant cutoff) {
        return createQuery(SELECT_ACTIVE_OLDER, Match.class)
                .setParameter("state", MatchState.ACTIVE)
                .setParameter("cutoff", cutoff)
                .getResultList();
    }

    public Match selectActiveByRoomId(Long roomId) {
        return selectSingleOrNull(createQuery(SELECT_ACTIVE_BY_ROOM, Match.class)
                .setParameter("roomId", roomId)
                .setParameter("state", MatchState.ACTIVE));
    }
}
