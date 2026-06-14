package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileData {

    private Long id;
    private String email;
    private String displayName;
    private String avatarUrl;
}
