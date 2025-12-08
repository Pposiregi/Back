package com.fitpet.server.badge.application.mapper;

import com.fitpet.server.badge.domain.entity.Badge;
import com.fitpet.server.badge.presentation.dto.BadgeCreateRequest;
import com.fitpet.server.badge.presentation.dto.BadgeDto;
import com.fitpet.server.badge.presentation.dto.BadgeUpdateRequest;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface BadgeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Badge toEntity(BadgeCreateRequest request);

    @Mapping(target = "badgeId", source = "id")
    BadgeDto toDto(Badge badge);

    List<BadgeDto> toDtos(List<Badge> badges);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Badge badge, BadgeUpdateRequest request);
}
