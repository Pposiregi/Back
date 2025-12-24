package com.fitpet.server.dailyworkout.Infra.jpa;

import com.fitpet.server.dailyworkout.domain.entity.GpsLog;
import com.fitpet.server.dailyworkout.domain.entity.GpsSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsLogJpaRepository extends JpaRepository<GpsLog, Long> {
    List<GpsLog> findByGpsSessionOrderByRecordedAtAsc(GpsSession gpsSession);
    
}