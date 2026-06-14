package com.coduel.api;

import com.coduel.common.api.AbstractApi;
import com.coduel.common.exception.ApiException;
import com.coduel.dao.LeaderboardDao;
import com.coduel.entity.Leaderboard;
import com.coduel.helper.ConversionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(rollbackFor = ApiException.class)
public class LeaderboardApi extends AbstractApi {

    @Autowired
    private LeaderboardDao leaderboardDao;

    public Leaderboard getOrCreate(Long userId) {
        Leaderboard leaderboard = leaderboardDao.selectByUserId(userId);
        return Objects.nonNull(leaderboard) ? leaderboard : leaderboardDao.persist(ConversionHelper.toLeaderboard(userId));
    }

    // Managed entity -> the increment is dirty-checked and flushed on commit.
    public void recordWin(Long userId) {
        Leaderboard leaderboard = getOrCreate(userId);
        leaderboard.setWins(leaderboard.getWins() + 1);
    }

    public void recordLoss(Long userId) {
        Leaderboard leaderboard = getOrCreate(userId);
        leaderboard.setLosses(leaderboard.getLosses() + 1);
    }

    // Read-only lookup (nullable) -> used by search so we never persist a row for an un-played user.
    public Leaderboard get(Long userId) {
        return leaderboardDao.selectByUserId(userId);
    }

    public List<Leaderboard> getPage(int page, int size) {
        return leaderboardDao.selectPageByWins(page, size);
    }

    public long count() {
        return leaderboardDao.count();
    }
}
