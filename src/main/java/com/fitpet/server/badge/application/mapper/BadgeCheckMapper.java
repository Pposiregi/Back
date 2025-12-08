package com.fitpet.server.badge.application.mapper;

import com.fitpet.server.badge.domain.entity.Badge;
import com.fitpet.server.badge.domain.entity.BadgeCheck;
import com.fitpet.server.badge.presentation.dto.BadgeCheckDto;
import com.fitpet.server.user.domain.entity.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BadgeCheckMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "badge", source = "badge")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BadgeCheck toEntity(User user, Badge badge);

    @Mapping(target = "badgeCheckId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "badgeId", source = "badge.id")
    BadgeCheckDto toDto(BadgeCheck badgeCheck);

    List<BadgeCheckDto> toDtos(List<BadgeCheck> badgeChecks);
}
