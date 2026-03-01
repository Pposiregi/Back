package com.fitpet.server.user.application.dto;

import com.fitpet.server.pet.domain.entity.PetExpression;
import com.fitpet.server.pet.domain.entity.PetType;
import lombok.Builder;

@Builder
public record PetSummaryResult(
    Long petId,
    String name,
    PetType petType,
    String color,
    Long exp,
    PetExpression expression
) {
}
