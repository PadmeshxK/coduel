package com.coduel.flow;

import com.coduel.api.LeaderboardApi;
import com.coduel.api.UserApi;
import com.coduel.common.data.PageData;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Leaderboard;
import com.coduel.entity.User;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.result.LeaderboardEntryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Transactional(rollbackFor = ApiException.class)
public class LeaderboardFlow {

    @Autowired
    private LeaderboardApi leaderboardApi;
    @Autowired
    private UserApi userApi;

    // Ranked page: each leaderboard row joined with its user profile (two Apis → a Flow).
    public PageData<LeaderboardEntryResult> getPage(int page, int size) throws ApiException {
        List<LeaderboardEntryResult> content = new ArrayList<>();
        for (Leaderboard leaderboard : leaderboardApi.getPage(page, size)) {
            content.add(new LeaderboardEntryResult(userApi.getCheckById(leaderboard.getUserId()), leaderboard));
        }
        return ConversionHelper.toPage(content, page, size, leaderboardApi.count());
    }

    // Name-prefix search off the User table; stats joined per hit (transient 0-0 for un-played players).
    public List<LeaderboardEntryResult> search(String query, int limit) {
        List<LeaderboardEntryResult> content = new ArrayList<>();
        for (User user : userApi.searchByDisplayNamePrefix(query, limit)) {
            Leaderboard leaderboard = leaderboardApi.get(user.getId());
            if (Objects.isNull(leaderboard)) {
                leaderboard = ConversionHelper.toLeaderboard(user.getId()); // transient zeros, not persisted
            }
            content.add(new LeaderboardEntryResult(user, leaderboard));
        }
        return content;
    }
}
