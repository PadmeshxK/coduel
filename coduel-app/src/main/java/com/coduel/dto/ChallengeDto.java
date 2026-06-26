package com.coduel.dto;

import com.coduel.api.UserApi;
import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.entity.User;
import com.coduel.flow.ChallengeFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.NotificationInbox;
import com.coduel.interfaces.UserNotificationPublisher;
import com.coduel.model.constant.Errors;
import com.coduel.model.data.ChallengeData;
import com.coduel.model.data.NotificationData;
import com.coduel.model.result.ChallengeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ChallengeDto {

    @Autowired
    private ChallengeFlow challengeFlow;
    @Autowired
    private NotificationInbox notificationInbox;
    @Autowired
    private UserNotificationPublisher userNotificationPublisher;
    @Autowired
    private UserApi userApi;

    // Flow verifies + resolves; the Dto owns the external bits (Redis store + the live push).
    public ChallengeData create(String challengerGoogleId, Long targetUserId, String problemSlug) throws ApiException {
        ChallengeResult result = challengeFlow.create(challengerGoogleId, targetUserId, problemSlug);
        String challengeId = UUID.randomUUID().toString();
        NotificationData challenge = ConversionHelper.toDuelChallengeNotification(challengeId, result.getChallenger(), problemSlug);
        notificationInbox.add(result.getOpponent().getGoogleId(), challenge);
        userNotificationPublisher.publish(result.getOpponent().getGoogleId(), challenge);
        return ConversionHelper.toChallengeData(challengeId, null, result.getOpponent().getDisplayName());
    }

    public ChallengeData accept(String challengeId, String acceptorGoogleId) throws ApiException {
        NotificationData challenge = requireChallenge(acceptorGoogleId, challengeId);
        // Atomically claim the challenge — if the challenger withdrew it at the same instant, our delete
        // loses the race and we abort before creating a match, so a cancelled duel never starts.
        if (!notificationInbox.removeIfPresent(acceptorGoogleId, challengeId)) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_128, List.of());
        }
        ChallengeResult result = challengeFlow.accept(challenge.getFromUserId(), acceptorGoogleId, challenge.getProblemSlug());
        // Send BOTH players into the duel — each told who the opponent is.
        userNotificationPublisher.publish(result.getChallenger().getGoogleId(),
                ConversionHelper.toChallengeAcceptedNotification(result.getMatchId(), result.getOpponent()));
        userNotificationPublisher.publish(result.getOpponent().getGoogleId(),
                ConversionHelper.toChallengeAcceptedNotification(result.getMatchId(), result.getChallenger()));
        return ConversionHelper.toChallengeData(null, result.getMatchId(), result.getChallenger().getDisplayName());
    }

    // The challenger withdraws a pending challenge they sent to targetUserId. Removes it from the
    // target's inbox so they can no longer accept a cancelled duel, and pushes a live signal so their
    // popup/bell drops it. Idempotent: a gone challenge (accepted/declined/expired) is a silent no-op.
    public void cancel(String challengeId, Long targetUserId, String challengerGoogleId) throws ApiException {
        User challenger = userApi.getCheckByGoogleId(challengerGoogleId);
        User target = userApi.getCheckById(targetUserId);
        NotificationData challenge = notificationInbox.get(target.getGoogleId(), challengeId);
        // Only the challenger who sent it can withdraw it (and only while it's still pending).
        if (challenge == null || !challenger.getId().equals(challenge.getFromUserId())) {
            return;
        }
        // Atomically claim it — if the target accepted at the same instant, our delete loses the race
        // and we don't push a (false) withdrawal: the accept already created the match and pulled both
        // players in. Exactly one of accept/cancel wins.
        if (!notificationInbox.removeIfPresent(target.getGoogleId(), challengeId)) {
            return;
        }
        userNotificationPublisher.publish(target.getGoogleId(),
                ConversionHelper.toChallengeWithdrawnNotification(challengeId));
    }

    public void decline(String challengeId, String declinerGoogleId) throws ApiException {
        NotificationData challenge = requireChallenge(declinerGoogleId, challengeId);
        notificationInbox.remove(declinerGoogleId, challengeId);
        ChallengeResult result = challengeFlow.decline(challenge.getFromUserId(), declinerGoogleId);
        userNotificationPublisher.publish(result.getChallenger().getGoogleId(),
                ConversionHelper.toChallengeDeclinedNotification(result.getOpponent()));
    }

    // The challenge must still exist in the target's bucket — gone means expired or already answered.
    private NotificationData requireChallenge(String targetGoogleId, String challengeId) throws ApiException {
        NotificationData challenge = notificationInbox.get(targetGoogleId, challengeId);
        if (challenge == null) {
            throw new ApiException(ApiStatus.NOT_FOUND, Errors.ERR_128, List.of());
        }
        return challenge;
    }
}
