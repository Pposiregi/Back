package com.fitpet.server.mission.application.mapper;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.MissionType;
import com.fitpet.server.mission.presentation.dto.MissionCheckDto;
import com.fitpet.server.user.domain.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MissionCheckMapper {

    // Entity -> DTO
    @Mapping(target = "missionCheckId", source = "id")
    @Mapping(target = "missionId", source = "mission.id")
    @Mapping(target = "userId", source = "user.id")
    MissionCheckDto toDto(MissionCheck missionCheck);

    List<MissionCheckDto> toDtos(List<MissionCheck> missionChecks);

    default MissionCheck create(
            Mission mission,
            User user,
            MissionType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal progressValue,
            boolean completed,
            LocalDateTime completedAt
    ) {
        return MissionCheck.builder()
                .mission(mission)
                .user(user)
                .periodType(periodType)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .progressValue(progressValue)
                .completed(completed)
                .completedAt(completedAt)
                .build();
    }
}