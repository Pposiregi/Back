package com.fitpet.server.shared.s3.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageType {
    MEAL("meal"),       // 식단 이미지
    PROFILE("profile"); // 프로필 이미지

    private final String path;
}