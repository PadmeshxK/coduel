package com.coduel.dao;

import com.coduel.entity.Match;
import org.springframework.stereotype.Repository;

@Repository
public class MatchDao extends BaseDao<Match> {

    public MatchDao() {
        super(Match.class);
    }
}
