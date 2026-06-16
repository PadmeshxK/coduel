package com.coduel.model.constant;

// Why a match ended. Set on the Match entity (durable) and carried in the MATCH_OVER event (live),
// so a client knows whether it was a real solve, a forfeit, a no-show, or a timeout — and can show
// the right message to the winner, the loser, or both (when there is no winner).
public enum MatchEndReason {

    // a player passed all tests (winnerUserId set)
    SOLVED,
    // a player disconnected past the grace window or hit "give up" (opponent wins)
    OPPONENT_FORFEIT,
    // a player never joined within the start grace (the one who showed wins by walkover)
    OPPONENT_NO_SHOW,
    // neither player joined — match voided (no winner)
    NO_SHOW_VOID,
    // match exceeded its TTL unsolved (no winner)
    TIMEOUT
}
