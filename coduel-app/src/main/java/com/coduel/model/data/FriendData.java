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
}
