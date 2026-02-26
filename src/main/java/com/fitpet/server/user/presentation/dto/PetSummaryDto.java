package com.fitpet.server.user.presentation.dto;

import com.fitpet.server.pet.domain.entity.PetExpression;
import com.fitpet.server.pet.domain.entity.PetType;

public record PetSummaryDto(
        Long petId,
        String name,
        PetType petType,
        String color,
        Long exp,
        PetExpression expression
) {
}
