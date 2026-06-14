package com.coduel.dto;

import com.coduel.api.LeaderboardApi;
import com.coduel.api.UserApi;
import com.coduel.common.data.PageData;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Leaderboard;
import com.coduel.entity.User;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.LeaderboardData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class LeaderboardDto {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int SEARCH_LIMIT = 20;

    @Autowired
    private LeaderboardApi leaderboardApi;
    @Autowired
    private UserApi userApi;

    public PageData<LeaderboardData> getPage(int page, int size) throws ApiException {
        int bounded = Math.clamp(size, 1, MAX_PAGE_SIZE);
        List<LeaderboardData> content = new ArrayList<>();
        for (Leaderboard leaderboard : leaderboardApi.getPage(page, bounded)) {
            content.add(ConversionHelper.toLeaderboardData(userApi.getCheckById(leaderboard.getUserId()), leaderboard));
        }
        return ConversionHelper.toPage(content, page, bounded, leaderboardApi.count());
    }

    // Name-prefix search: matched off the User table, stats joined per hit (0-0 for un-played players).
    public List<LeaderboardData> search(String query) {
        List<LeaderboardData> content = new ArrayList<>();
        for (User user : userApi.searchByDisplayNamePrefix(query, SEARCH_LIMIT)) {
            Leaderboard leaderboard = leaderboardApi.get(user.getId());
            if (Objects.isNull(leaderboard)) {
                leaderboard = ConversionHelper.toLeaderboard(user.getId()); // transient zeros, not persisted
            }
            content.add(ConversionHelper.toLeaderboardData(user, leaderboard));
        }
        return content;
    }
}
