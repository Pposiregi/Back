package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.MissionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionCheckJpaRepository extends JpaRepository<MissionCheck, Long> {

    Optional<MissionCheck> findByMissionIdAndUserIdAndPeriodTypeAndPeriodStart(
        Long missionId,
        Long userId,
        MissionType periodType,
        LocalDate periodStart
    );

    List<MissionCheck> findAllByUserIdOrderByPeriodStartDesc(Long userId);

    @Query("""
        select mc
        from MissionCheck mc
        join fetch mc.mission m
        where mc.user.id = :userId
          and m.category = :category
          and mc.completed = false
          and :date between mc.periodStart and mc.periodEnd
        """)
    List<MissionCheck> findActiveByUserAndCategoryAndDate(
        @Param("userId") Long userId,
        @Param("category") MissionCategory category,
        @Param("date") LocalDate date
    );

    @Query("""
        select mc
        from MissionCheck mc
        join fetch mc.mission m
        where mc.user.id = :userId
          and mc.completed = false
          and :date between mc.periodStart and mc.periodEnd
        """)
    List<MissionCheck> findActiveByUserAndDate(
        @Param("userId") Long userId,
        @Param("date") LocalDate date
    );

    @Query("""
        select mc
        from MissionCheck mc
        join fetch mc.mission m
        where mc.user.id = :userId
          and mc.completed = true
        order by mc.completedAt desc
        """)
    List<MissionCheck> findCompletedByUser(@Param("userId") Long userId);
}
