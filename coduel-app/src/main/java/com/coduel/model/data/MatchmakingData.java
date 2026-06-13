package com.coduel.model.data;

import com.coduel.model.constant.MatchmakingStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatchmakingData {

    private MatchmakingStatus status;
    // null while WAITING; set once MATCHED.
    private Long matchId;
    private Long problemId;
}
