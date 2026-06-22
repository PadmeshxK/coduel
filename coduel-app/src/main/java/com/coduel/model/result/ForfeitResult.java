package com.coduel.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Outcome of a forfeit: who dropped out, and the winner if it ended the match (null if it plays on).
@Getter
@AllArgsConstructor
public class ForfeitResult {

    private Long forfeiterUserId;
    private Long winnerUserId;
}
