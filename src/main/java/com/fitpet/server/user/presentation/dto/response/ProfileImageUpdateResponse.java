package com.fitpet.server.user.presentation.dto.response;

import com.fitpet.server.user.application.dto.ProfileImageUpdateResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileImageUpdateResponse {
    private String imageKey;
    private String uploadUrl;

    public static ProfileImageUpdateResponse from(ProfileImageUpdateResult result) {
        if (result == null) {
            return null;
        }
        return new ProfileImageUpdateResponse(result.imageKey(), result.uploadUrl());
    }
}
