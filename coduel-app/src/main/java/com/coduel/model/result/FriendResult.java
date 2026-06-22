package com.coduel.model.result;

import com.coduel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

// A user search hit plus the caller's relationship to them: already friends, or a still-pending
// request the caller has sent. The Dto maps this to FriendData (friend / pending drive the UI action).
@Getter
@AllArgsConstructor
public class FriendResult {

    private User user;
    private boolean friend;
    private boolean pending;
}
