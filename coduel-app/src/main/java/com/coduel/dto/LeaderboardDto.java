package com.coduel.dto;

import com.coduel.common.data.PageData;
import com.coduel.common.exception.ApiException;
import com.coduel.flow.LeaderboardFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.LeaderboardData;
import com.coduel.model.result.LeaderboardEntryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LeaderboardDto {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int SEARCH_LIMIT = 20;

    @Autowired
    private LeaderboardFlow leaderboardFlow;

    public PageData<LeaderboardData> getPage(int page, int size) throws ApiException {
        int bounded = Math.clamp(size, 1, MAX_PAGE_SIZE);
        PageData<LeaderboardEntryResult> entries = leaderboardFlow.getPage(page, bounded);
        List<LeaderboardData> content = entries.getContent().stream().map(LeaderboardDto::toData).toList();
        return ConversionHelper.toPage(content, entries.getPage(), entries.getSize(), entries.getTotalElements());
    }

    public List<LeaderboardData> search(String query) {
        return leaderboardFlow.search(query, SEARCH_LIMIT).stream().map(LeaderboardDto::toData).toList();
    }

    private static LeaderboardData toData(LeaderboardEntryResult entry) {
        return ConversionHelper.toLeaderboardData(entry.getUser(), entry.getLeaderboard());
    }
}
