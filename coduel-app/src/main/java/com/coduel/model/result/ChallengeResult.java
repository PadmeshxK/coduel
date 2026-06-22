package com.coduel.model.result;

import com.coduel.entity.User;
import lombok.Getter;
import lombok.Setter;

// Flow → Dto carrier for any challenge action. challenger + opponent are always set (opponent = the
// other party: the target on create, the accepter/decliner otherwise). challengeId is set on create,
// matchId on accept — so the Dto knows what to publish / return.
@Getter
@Setter
public class ChallengeResult {

    private String challengeId;
    private Long matchId;
    private User challenger;
    private User opponent;
}
