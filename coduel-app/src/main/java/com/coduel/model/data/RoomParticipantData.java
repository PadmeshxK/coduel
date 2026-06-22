package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomParticipantData {

    private Long userId;
    private String displayName;
    private String avatarUrl;
    private boolean host;
    private boolean ready;
}
