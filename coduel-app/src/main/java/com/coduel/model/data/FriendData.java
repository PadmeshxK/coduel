package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

// Public view of a user (no email): a friend, or a search hit you can add.
@Getter
@Setter
public class FriendData {

    private Long userId;
    private String displayName;
    private String avatarUrl;
    // Search-result relationship flags (false for plain friend-list rows): already friends, or I've
    // sent a still-pending request. The UI shows "Friend" / "Requested" instead of "Add".
    private boolean friend;
    private boolean pending;
    // Friend-list rows only: when the friendship began (epoch millis), so the UI can show "Friends for…".
    private Long friendsSinceMs;
}
