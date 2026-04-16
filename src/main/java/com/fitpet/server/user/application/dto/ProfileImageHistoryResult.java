package com.fitpet.server.user.application.dto;

public record ProfileImageHistoryResult(
        String imageKey,
        String presignedUrl
) {
}
