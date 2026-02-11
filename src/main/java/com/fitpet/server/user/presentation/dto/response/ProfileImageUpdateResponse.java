package com.fitpet.server.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileImageUpdateResponse {
    private String imageKey;
    private String uploadUrl;
}