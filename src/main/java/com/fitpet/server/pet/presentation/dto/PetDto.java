package com.fitpet.server.pet.presentation.dto;

import com.fitpet.server.pet.domain.entity.PetExpression;
import com.fitpet.server.pet.domain.entity.PetType;
import java.time.LocalDateTime;

public record PetDto(
    Long petId,
    Long ownerId,
    String name,
    PetType petType,
    String color,
    Long exp,
    PetExpression expression,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
