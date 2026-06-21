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
    // Whether the user has chosen a unique display name — false routes the SPA to the setup page.
    private boolean displayNameSet;
}
