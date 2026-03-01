package com.fitpet.server.user.application.dto;

public record ProfileImageUpdateResult(
    String imageKey,
    String uploadUrl
) {
}
