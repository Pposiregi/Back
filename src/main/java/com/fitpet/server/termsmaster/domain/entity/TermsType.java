package com.fitpet.server.termsmaster.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TermsType {
    SERVICE_USE("서비스 이용약관", true),
    PRIVACY_COLLECTION("개인정보 수집·이용 동의서", true),
    PRIVACY_POLICY("개인정보 처리방침", true),
    LOCATION_BASED("위치기반서비스 이용약관", true),
    HEALTH_INFO("건강정보(민감정보) 수집·이용 동의서", true),
    MARKETING("마케팅 정보 수신 동의서", false);

    private final String description;
    private final boolean required;
}
