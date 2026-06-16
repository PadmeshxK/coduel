package com.coduel.model.data;

import com.coduel.model.constant.MatchEndReason;
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
    // null while live; how the match ended, so a reconnecting/refreshing client sees the right outcome.
    private MatchEndReason endReason;
    // Epoch millis. Source of truth for the elapsed clock — both clients compute from these,
    // so a refresh never resets it. endedAtMs is null while the match is live.
    private Long startedAtMs;
    private Long endedAtMs;
    private List<MatchParticipantData> participants;
}
