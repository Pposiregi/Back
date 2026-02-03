package com.fitpet.server.dailyworkout.application.mapper;

import com.fitpet.server.dailyworkout.domain.entity.GpsLog;
import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import com.fitpet.server.dailyworkout.presentation.dto.request.GpsLogRequest;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsLogResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.GpsSessionStartResponse;
import com.fitpet.server.dailyworkout.presentation.dto.response.SessionEndResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface GpsMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "gpsSession", source = "session")
    @Mapping(target = "user", source = "session.user")
    GpsLog toGpsLogEntity(GpsLogRequest request, GpsSession session);

    @Mapping(source = "id", target = "sessionId")
    @Mapping(target = "message", constant = "GPS session started")
    GpsSessionStartResponse toSessionStartResponse(GpsSession session);

    @Mapping(source = "id", target = "logId")
    @Mapping(source = "gpsSession.id", target = "sessionId")
    @Mapping(target = "message", constant = "GPS log saved")
    GpsLogResponse toGpsLogResponse(GpsLog log);

    @Mapping(source = "id", target = "sessionId")
    @Mapping(target = "message", constant = "GPS session ended")
    SessionEndResponse toSessionEndResponse(GpsSession session);
}