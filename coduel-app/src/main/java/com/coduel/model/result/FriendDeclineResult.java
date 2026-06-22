package com.coduel.model.result;

import com.coduel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Result of dropping a friendship row. requesterToNotify is set only when the action was the
// addressee declining a still-pending request — the original requester is told (silently) so their
// "Requested" button reverts to "Add". decliner is the actor (the "from" of that push). A null
// requesterToNotify means no one is notified (e.g. a requester cancelling their own request).
@Getter
@AllArgsConstructor
public class FriendDeclineResult {

    private User requesterToNotify;
    private User decliner;
}
