package com.fitpet.server.pet.application.mapper;

import com.fitpet.server.pet.domain.entity.Pet;
import com.fitpet.server.pet.presentation.dto.PetCreateRequest;
import com.fitpet.server.pet.presentation.dto.PetDto;
import com.fitpet.server.user.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PetMapper {

    // 생성: 요청 + 소유자(User) -> 엔티티
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "exp", expression = "java(0L)"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "color", source = "request.color"),
            @Mapping(target = "owner", source = "owner")   // 소유자
    })
    Pet toEntity(PetCreateRequest request, User owner);

    // 엔티티 -> DTO
    @Mappings({
            @Mapping(target = "petId", source = "id"),
            @Mapping(target = "ownerId", source = "owner.id"),
            @Mapping(target = "color", source = "color"),
            @Mapping(target = "expression", source = "expression")
    })
    PetDto toDto(Pet pet);
}
