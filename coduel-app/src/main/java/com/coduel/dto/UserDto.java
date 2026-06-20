package com.coduel.dto;

import com.coduel.api.UserApi;
import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.FriendData;
import com.coduel.model.data.UserProfileData;
import com.coduel.model.form.UserProfileForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class UserDto extends AbstractDto {

    private static final int SEARCH_LIMIT = 20;

    @Autowired
    private UserApi userApi;

    public UserProfileData getProfile(String googleId) throws ApiException {
        return ConversionHelper.toUserProfileData(userApi.getCheckByGoogleId(googleId));
    }

    // Find people to add as friends by display-name prefix (public view — no emails).
    public List<FriendData> search(String query) {
        return userApi.searchByDisplayNamePrefix(query, SEARCH_LIMIT).stream()
                .map(ConversionHelper::toFriendData)
                .toList();
    }

    @Transactional(rollbackFor = ApiException.class)
    public UserProfileData updateProfile(UserProfileForm form, String googleId) throws ApiException {
        checkValid(form);
        trim(form);
        // Blank avatar -> null so the UI falls back to initials.
        String avatarUrl = (form.getAvatarUrl() == null || form.getAvatarUrl().isBlank())
                ? null
                : form.getAvatarUrl();
        return ConversionHelper.toUserProfileData(
                userApi.updateProfile(googleId, form.getDisplayName(), avatarUrl));
    }
}
