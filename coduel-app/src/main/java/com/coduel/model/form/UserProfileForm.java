package com.coduel.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileForm {

    @NotBlank(message = "displayName must not be blank")
    @Size(max = 50, message = "displayName must be at most 50 characters")
    private String displayName;

    // Optional — image URL (Google picture, a generated avatar, or custom). Blank clears it (UI falls back to initials).
    @Size(max = 512, message = "avatarUrl must be at most 512 characters")
    private String avatarUrl;
}
