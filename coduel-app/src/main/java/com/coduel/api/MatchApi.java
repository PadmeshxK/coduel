package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.MatchDao;
import com.coduel.entity.Match;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.MatchState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class MatchApi extends AbstractApi {

    @Autowired
    private MatchDao matchDao;

    public Match add(Match match) {
        return matchDao.persist(match);
    }

    public Match getCheckById(Long id) throws ApiException {
        Match match = matchDao.selectById(id);
        if (Objects.isNull(match)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_110, List.of(id));
        }
        return match;
    }

    // First ACCEPTED wins; idempotent. Returns true only on the ACTIVE -> FINISHED transition.
    public boolean finish(Long id, Long winnerUserId) throws ApiException {
        Match match = getCheckById(id);
        if (match.getState() != MatchState.ACTIVE) {
            return false;
        }
        match.setWinnerUserId(winnerUserId);
        match.setState(MatchState.FINISHED);
        match.setEndedAt(Instant.now());
        return true;
    }
}
