package com.coduel.flow;

import com.coduel.api.FriendshipApi;
import com.coduel.api.ProblemApi;
import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.Friendship;
import com.coduel.entity.Match;
import com.coduel.entity.Problem;
import com.coduel.entity.User;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.constant.Errors;
import com.coduel.model.constant.FriendshipStatus;
import com.coduel.model.constant.GameMode;
import com.coduel.model.result.ChallengeResult;
import com.coduel.websocket.MatchPresenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@Transactional(rollbackFor = ApiException.class)
public class ChallengeFlow {

    @Autowired
    private UserApi userApi;
    @Autowired
    private FriendshipApi friendshipApi;
    @Autowired
    private ProblemApi problemApi;
    @Autowired
    private MatchFlow matchFlow;
    @Autowired
    private MatchPresenceService matchPresenceService;

    // Verify a challenge is allowed (not yourself, must be friends) and resolve both parties. The Dto
    // stores the pending challenge in Redis + notifies after commit — the flow only orchestrates Apis.
    public ChallengeResult create(String challengerGoogleId, Long targetUserId) throws ApiException {
        User challenger = userApi.getCheckByGoogleId(challengerGoogleId);
        if (challenger.getId().equals(targetUserId)) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_129, List.of());
        }
        Friendship friendship = friendshipApi.findBetween(challenger.getId(), targetUserId);
        if (Objects.isNull(friendship) || friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new ApiException(ApiStatus.FORBIDDEN, Errors.ERR_127, List.of());
        }
        User target = userApi.getCheckById(targetUserId);
        return ConversionHelper.toChallengeResult(null, null, challenger, target);
    }

    // Build the duel between the two parties (same start-grace / no-show rule as matchmaking).
    public ChallengeResult accept(Long challengerUserId, String acceptorGoogleId) throws ApiException {
        User accepter = userApi.getCheckByGoogleId(acceptorGoogleId);
        User challenger = userApi.getCheckById(challengerUserId);
        Problem problem = problemApi.getCheckRandomProblem();
        Match match = matchFlow.create(GameMode.DUEL, null, problem.getId(),
                List.of(challenger.getId(), accepter.getId()));
        matchPresenceService.scheduleStartDeadline(match.getId());
        return ConversionHelper.toChallengeResult(null, match.getId(), challenger, accepter);
    }

    // Resolve both parties so the Dto can notify the challenger that their challenge was declined.
    public ChallengeResult decline(Long challengerUserId, String declinerGoogleId) throws ApiException {
        User decliner = userApi.getCheckByGoogleId(declinerGoogleId);
        User challenger = userApi.getCheckById(challengerUserId);
        return ConversionHelper.toChallengeResult(null, null, challenger, decliner);
    }
}
