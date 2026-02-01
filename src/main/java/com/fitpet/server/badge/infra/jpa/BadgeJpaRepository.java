package com.fitpet.server.badge.infra.jpa;

import com.fitpet.server.badge.domain.entity.Badge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BadgeJpaRepository extends JpaRepository<Badge, Long> {
    
    @Query("""
            select b
            from Badge b
            where b.mission.id = :missionId
              and b.conditionGoal <= :clearCount
            order by b.conditionGoal desc
            """)
    List<Badge> findEligibleByMissionIdAndClearCount(
            @Param("missionId") Long missionId,
            @Param("clearCount") Long clearCount);
}
