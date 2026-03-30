package com.fitpet.server.mission.application.mapper;

import com.fitpet.server.mission.application.dto.MissionCheckResult;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MissionCheckMapper {

    // Entity -> DTO
    @Mapping(target = "missionCheckId", source = "id")
    @Mapping(target = "missionId", source = "mission.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "clearCount", ignore = true)
    MissionCheckResult toDto(MissionCheck missionCheck);

    List<MissionCheckResult> toDtos(List<MissionCheck> missionChecks);
}