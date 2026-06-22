package com.coduel.model.result;

import com.coduel.entity.Friendship;
import com.coduel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Internal carrier: a pending friend request joined with the requester's profile. */
@Getter
@AllArgsConstructor
public class IncomingFriendRequestResult {

    private Friendship friendship;
    private User requester;
}
