package com.fitpet.server.mission.application.mapper;

import com.fitpet.server.mission.domain.entity.Mission;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.presentation.dto.MissionCheckDto;
import com.fitpet.server.mission.presentation.dto.MissionCheckRequest;
import com.fitpet.server.user.domain.entity.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MissionCheckMapper {

    // Entity -> DTO
    @Mapping(target = "missionCheckId", source = "id")
    @Mapping(target = "missionId", source = "mission.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "checkDate", source = "checkAt")
    MissionCheckDto toDto(MissionCheck missionCheck);

    List<MissionCheckDto> toDtos(List<MissionCheck> missionChecks);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mission", source = "mission")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "completed", source = "request.completed")
    @Mapping(target = "checkAt", source = "request.checkDate")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MissionCheck create(Mission mission, User user, MissionCheckRequest request);

    default MissionCheck updateFromRequest(@MappingTarget MissionCheck target, MissionCheckRequest request) {
        target.updateCompletion(request.completed(), request.checkDate());
        return target;
    }
}