package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.MatchParticipantDao;
import com.coduel.entity.MatchParticipant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = ApiException.class)
public class MatchParticipantApi extends AbstractApi {

    @Autowired
    private MatchParticipantDao matchParticipantDao;

    public MatchParticipant add(MatchParticipant participant) {
        return matchParticipantDao.persist(participant);
    }

    public List<MatchParticipant> getByMatchId(Long matchId) {
        return matchParticipantDao.selectByMatchId(matchId);
    }

    public List<MatchParticipant> getByUserId(Long userId) {
        return matchParticipantDao.selectByUserId(userId);
    }
}
