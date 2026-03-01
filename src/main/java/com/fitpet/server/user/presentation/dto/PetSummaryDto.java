package com.fitpet.server.user.presentation.dto;

import com.fitpet.server.pet.domain.entity.PetExpression;
import com.fitpet.server.pet.domain.entity.PetType;
import com.fitpet.server.user.application.dto.PetSummaryResult;
import lombok.Builder;

@Builder
public record PetSummaryDto(
        Long petId,
        String name,
        PetType petType,
        String color,
        Long exp,
        PetExpression expression
) {
    public static PetSummaryDto from(PetSummaryResult result) {
        if (result == null) {
            return null;
        }

        return PetSummaryDto.builder()
            .petId(result.petId())
            .name(result.name())
            .petType(result.petType())
            .color(result.color())
            .exp(result.exp())
            .expression(result.expression())
            .build();
    }
}
