package com.fitpet.server.dailyworkout.domain.repository;

import com.fitpet.server.dailyworkout.domain.entity.GpsLog;
import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import java.util.List;

public interface GpsLogRepository {
    GpsLog save(GpsLog gpsLog);

    List<GpsLog> findByGpsSessionOrderByRecordedAtAsc(GpsSession gpsSession);

    GpsLog findTopByGpsSessionOrderByRecordedAtDesc(GpsSession gpsSession);
}