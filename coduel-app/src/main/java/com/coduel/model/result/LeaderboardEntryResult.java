package com.coduel.model.result;

import com.coduel.entity.Leaderboard;
import com.coduel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Internal carrier: a user joined with their leaderboard row. */
@Getter
@AllArgsConstructor
public class LeaderboardEntryResult {

    private User user;
    private Leaderboard leaderboard;
}
