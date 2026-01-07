package com.fitpet.server.mission.application.mapper;

import com.fitpet.server.mission.application.dto.MissionCreateCommand;
import com.fitpet.server.mission.application.dto.MissionResult;
import com.fitpet.server.mission.domain.entity.Mission;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MissionMapper {

    // Entity -> DTO
    @Mapping(target = "missionId", source = "id")
    MissionResult toDto(Mission mission);

    List<MissionResult> toDtos(List<Mission> missions);

    // CreateRequest -> Entity (신규 생성)
    @Mapping(target = "id", ignore = true)
    Mission toEntity(MissionCreateCommand request);

}