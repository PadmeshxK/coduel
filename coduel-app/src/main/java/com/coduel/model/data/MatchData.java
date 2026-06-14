package com.coduel.model.data;

import com.coduel.model.constant.MatchState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MatchData {

    private Long matchId;
    private MatchState state;
    private String slug;
    private String problemTitle;
    // null until the match is decided.
    private Long winnerUserId;
    private List<MatchParticipantData> participants;
}
