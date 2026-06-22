package com.coduel.dto;

import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.flow.UserFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.FriendData;
import com.coduel.model.data.UserProfileData;
import com.coduel.model.form.UserProfileForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserDto extends AbstractDto {

    @Autowired
    private UserFlow userFlow;

    public UserProfileData getProfile(String googleId) throws ApiException {
        return ConversionHelper.toUserProfileData(userFlow.getByGoogleId(googleId));
    }

    // Public directory search (no emails) — each hit carries the caller's relationship to it.
    public List<FriendData> search(String query, String googleId) throws ApiException {
        return userFlow.searchByUserPrefix(query, googleId).stream()
                .map(ConversionHelper::toFriendData)
                .toList();
    }

    public boolean isDisplayNameAvailable(String displayName, String googleId) throws ApiException {
        return userFlow.isDisplayNameAvailable(displayName, googleId);
    }

    public UserProfileData updateProfile(UserProfileForm form, String googleId) throws ApiException {
        checkValid(form);
        trim(form);
        // Blank avatar -> null so the UI falls back to initials.
        String avatarUrl = (form.getAvatarUrl() == null || form.getAvatarUrl().isBlank())
                ? null
                : form.getAvatarUrl();
        return ConversionHelper.toUserProfileData(
                userFlow.updateProfile(googleId, form.getDisplayName(), avatarUrl));
    }
}
