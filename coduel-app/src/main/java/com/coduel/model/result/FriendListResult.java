package com.coduel.model.result;

import com.coduel.entity.User;
import lombok.Getter;
import lombok.Setter;

// A friend plus when the friendship began — the Dto maps it to FriendData so the friends list can
// show "Friends for…". Built via ConversionHelper, not a constructor.
@Getter
@Setter
public class FriendListResult {

    private User friend;
    private Long sinceMs;
}
