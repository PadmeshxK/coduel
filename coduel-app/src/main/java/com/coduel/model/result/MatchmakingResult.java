package com.coduel.model.result;

import com.coduel.entity.Match;
import com.coduel.model.constant.MatchmakingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Internal carrier: the outcome of a matchmaking attempt — status, the match if paired, its slug,
 * the caller's userId (so the Dto can act on the queue without re-resolving it), and — only when a
 * fresh pairing was made — the waiting opponent's googleId, so the Dto can push them the match.
 */
@Getter
@AllArgsConstructor
public class MatchmakingResult {

    private MatchmakingStatus status;
    private Match match;
    private String slug;
    private Long userId;
    private String opponentGoogleId;
}
