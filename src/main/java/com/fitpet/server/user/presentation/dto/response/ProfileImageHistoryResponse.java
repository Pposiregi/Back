package com.fitpet.server.user.presentation.dto.response;

import com.fitpet.server.user.application.dto.ProfileImageHistoryResult;

public record ProfileImageHistoryResponse(
        String imageKey,
        String presignedUrl
) {
    public static ProfileImageHistoryResponse from(ProfileImageHistoryResult result) {
        return new ProfileImageHistoryResponse(result.imageKey(), result.presignedUrl());
    }
}
