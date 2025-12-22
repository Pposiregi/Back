package com.fitpet.server.termsmaster.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TermsType {
    SERVICE_USE("서비스 이용약관", true),
    PRIVACY_POLICY("개인정보 처리방침", true),
    MARKETING("마케팅 정보 수신 동의", false);

    private final String description;
    private final boolean required;
}
