package com.coduel.model.constant;

public enum MatchEventType {

    // both players are subscribed — the duel may begin
    MATCH_READY,
    SUBMISSION_JUDGED,
    // a player forfeited but the match continues (carries their userId); MATCH_OVER fires instead
    // when the forfeit leaves a single player standing.
    PLAYER_FORFEIT,
    MATCH_OVER
}
