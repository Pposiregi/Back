package com.fitpet.server.ranking.domain.entity;

public enum OutboxStatus {
    PENDING,
    CLAIMED,
    PUBLISHED,
    FAILED
}
