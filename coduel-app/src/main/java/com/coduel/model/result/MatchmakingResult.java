package com.coduel.model.result;

import com.coduel.entity.Match;
import com.coduel.model.constant.MatchmakingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Internal carrier: the outcome of a matchmaking poll — status, the match if paired, its slug, and
 * the caller's userId so the Dto can act on the queue (enqueue/remove) without re-resolving it.
 */
@Getter
@AllArgsConstructor
public class MatchmakingResult {

    private MatchmakingStatus status;
    private Match match;
    private String slug;
    private Long userId;
}
