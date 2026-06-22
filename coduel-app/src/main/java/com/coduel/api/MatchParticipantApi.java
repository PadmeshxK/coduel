package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.model.constant.Errors;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.MatchParticipantDao;
import com.coduel.entity.MatchParticipant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

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

    public MatchParticipant forfeitUserByMatchIdAndUserId(Long matchId, Long userId) throws ApiException{
        List<MatchParticipant> participants = getByMatchId(matchId);
        MatchParticipant participant = participants.stream()
                .filter(p -> Objects.equals(p.getUserId(), userId))
                .findFirst()
                .orElse(null);

        if (Objects.isNull(participant)) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_112, List.of(userId));
        }

        if(participant.isForfeit()){
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_125, List.of(userId, matchId));
        }

        participant.setForfeit(true);
        return participant;
    }

    public void delete(MatchParticipant participant) {
        matchParticipantDao.delete(participant);
    }
}
