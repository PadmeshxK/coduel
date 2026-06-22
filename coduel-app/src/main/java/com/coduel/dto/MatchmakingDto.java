package com.coduel.dto;

import com.coduel.common.exception.ApiException;
import com.coduel.flow.MatchmakingFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.MatchmakingQueue;
import com.coduel.interfaces.UserNotificationPublisher;
import com.coduel.model.constant.MatchmakingStatus;
import com.coduel.model.data.MatchmakingData;
import com.coduel.model.data.NotificationData;
import com.coduel.model.result.MatchmakingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MatchmakingDto {

    @Autowired
    private MatchmakingFlow matchmakingFlow;
    @Autowired
    private MatchmakingQueue matchmakingQueue;
    @Autowired
    private UserNotificationPublisher userNotificationPublisher;

    public MatchmakingData join(String googleId) throws ApiException {
        // Already in a duel (re-click, or paired while away)? Return it — never poll, or we'd consume
        // a waiting opponent for nothing.
        MatchmakingResult mine = matchmakingFlow.status(googleId);
        if (mine.getStatus() == MatchmakingStatus.MATCHED) {
            return toData(mine);
        }
        // Try to pair with whoever's waiting; if there's no usable opponent, take a spot in the queue.
        Long opponent = matchmakingQueue.poll();
        MatchmakingResult result = matchmakingFlow.pair(mine.getUserId(), opponent);
        if (result.getStatus() == MatchmakingStatus.WAITING) {
            matchmakingQueue.enqueue(mine.getUserId());
        } else {
            // Paired — push "match found" to BOTH players so they jump into the duel without polling.
            // (The caller also gets it from this HTTP response; the opponent was only waiting.)
            NotificationData found = ConversionHelper.toMatchmakingFoundNotification(result.getMatch().getId());
            userNotificationPublisher.publish(googleId, found);
            userNotificationPublisher.publish(result.getOpponentGoogleId(), found);
        }
        return toData(result);
    }

    public MatchmakingData status(String googleId) throws ApiException {
        return toData(matchmakingFlow.status(googleId));
    }

    // Cancel: drop the user from the queue (e.g. they navigated away while searching). No-op if absent.
    public void leave(String googleId) throws ApiException {
        matchmakingQueue.remove(matchmakingFlow.userIdOf(googleId));
    }

    private static MatchmakingData toData(MatchmakingResult result) {
        return ConversionHelper.toMatchmakingData(result.getStatus(), result.getMatch(), result.getSlug());
    }
}
