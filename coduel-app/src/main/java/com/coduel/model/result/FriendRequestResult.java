package com.coduel.model.result;

import com.coduel.entity.Friendship;
import com.coduel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Carries both users + the saved request so the Dto can push the notification (with full sender
// context) to the addressee after the transaction commits.
@Getter
@AllArgsConstructor
public class FriendRequestResult {

    private User requester;
    private User addressee;
    private Friendship friendship;
}
