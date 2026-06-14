package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaderboardData {

    private Long userId;
    private String displayName;
    private String avatarUrl;
    private int wins;
    private int losses;
}
