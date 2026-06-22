package com.coduel.dto;

import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
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

    // Flow verifies + resolves; the Dto owns the external bits (Redis store + the live push).
    public ChallengeData create(String challengerGoogleId, Long targetUserId) throws ApiException {
        ChallengeResult result = challengeFlow.create(challengerGoogleId, targetUserId);
        String challengeId = UUID.randomUUID().toString();
        NotificationData challenge = ConversionHelper.toDuelChallengeNotification(challengeId, result.getChallenger());
        notificationInbox.add(result.getOpponent().getGoogleId(), challenge);
        userNotificationPublisher.publish(result.getOpponent().getGoogleId(), challenge);
        return ConversionHelper.toChallengeData(challengeId, null, result.getOpponent().getDisplayName());
    }

    public ChallengeData accept(String challengeId, String acceptorGoogleId) throws ApiException {
        NotificationData challenge = requireChallenge(acceptorGoogleId, challengeId);
        notificationInbox.remove(acceptorGoogleId, challengeId);
        ChallengeResult result = challengeFlow.accept(challenge.getFromUserId(), acceptorGoogleId);
        // Send BOTH players into the duel — each told who the opponent is.
        userNotificationPublisher.publish(result.getChallenger().getGoogleId(),
                ConversionHelper.toChallengeAcceptedNotification(result.getMatchId(), result.getOpponent()));
        userNotificationPublisher.publish(result.getOpponent().getGoogleId(),
                ConversionHelper.toChallengeAcceptedNotification(result.getMatchId(), result.getChallenger()));
        return ConversionHelper.toChallengeData(null, result.getMatchId(), result.getChallenger().getDisplayName());
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
