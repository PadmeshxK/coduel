package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** A friend's online/offline transition, pushed live over /user/queue/presence. */
@Getter
@Setter
public class PresenceData {

    private Long userId;
    private boolean online;
}
