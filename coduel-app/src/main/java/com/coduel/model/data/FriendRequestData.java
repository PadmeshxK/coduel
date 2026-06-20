package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

// An incoming friend request: who sent it (public profile) + the request id to accept/decline.
@Getter
@Setter
public class FriendRequestData {

    private Long requestId;
    private Long userId;
    private String displayName;
    private String avatarUrl;
    private Long createdAtMs;
}
