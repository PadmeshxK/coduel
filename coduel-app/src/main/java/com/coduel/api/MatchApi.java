package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.MatchDao;
import com.coduel.entity.Match;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.MatchEndReason;
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

    // A winner is decided (solve / forfeit / walkout); idempotent. True only on ACTIVE -> FINISHED.
    public boolean markFinished(Long id, Long winnerUserId, MatchEndReason reason) throws ApiException {
        Match match = getCheckById(id);
        if (match.getState() != MatchState.ACTIVE) {
            return false;
        }
        match.setWinnerUserId(winnerUserId);
        match.setEndReason(reason);
        match.setState(MatchState.FINISHED);
        match.setEndedAt(Instant.now());
        return true;
    }

    // No winner (no-show void / timeout); idempotent. True only on the ACTIVE -> EXPIRED transition.
    public boolean expire(Long id, MatchEndReason reason) throws ApiException {
        Match match = getCheckById(id);
        if (match.getState() != MatchState.ACTIVE) {
            return false;
        }
        match.setEndReason(reason);
        match.setState(MatchState.EXPIRED);
        match.setEndedAt(Instant.now());
        return true;
    }

    public List<Match> getActiveOlderThan(Instant cutoff) {
        return matchDao.selectActiveOlderThan(cutoff);
    }
}
