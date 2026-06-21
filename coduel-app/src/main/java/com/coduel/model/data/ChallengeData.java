package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

// Response to the HTTP caller: challengeId on create (so the challenger can track/cancel + show the
// waiting state), matchId on accept (so the accepter navigates straight into the duel).
@Getter
@Setter
public class ChallengeData {

    private String challengeId;
    private Long matchId;
    private String opponentDisplayName;
}
