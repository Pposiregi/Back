package com.fitpet.server.badge.application.mapper;

import com.fitpet.server.badge.application.dto.BadgeCreateCommand;
import com.fitpet.server.badge.application.dto.BadgeResult;
import com.fitpet.server.badge.application.dto.BadgeUpdateCommand;
import com.fitpet.server.badge.domain.entity.Badge;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface BadgeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mission", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Badge toEntity(BadgeCreateCommand command);

    @Mapping(target = "badgeId", source = "id")
    @Mapping(target = "missionId", source = "mission.id")
    BadgeResult toResult(Badge badge);

    List<BadgeResult> toResults(List<Badge> badges);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Badge badge, BadgeUpdateCommand command);
}
