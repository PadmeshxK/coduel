package com.coduel.model.result;

import com.coduel.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InviteResult {

    private User fromUser;
    private User invitee;
}
