package com.fitpet.server.mission.infra.jpa;

import com.fitpet.server.mission.domain.entity.MissionCategory;
import com.fitpet.server.mission.domain.entity.MissionCheck;
import com.fitpet.server.mission.domain.entity.MissionType;
import com.fitpet.server.mission.domain.repository.MissionCheckKey;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionCheckJpaRepository extends JpaRepository<MissionCheck, Long> {

    @Query("""
            select mc
            from MissionCheck mc
            where mc.mission.id = :missionId
              and mc.user.id = :userId
              and mc.periodType = :periodType
              and mc.periodStart = :periodStart
            """)
    Optional<MissionCheck> findByPeriodKey(
            @Param("missionId") Long missionId,
            @Param("userId") Long userId,
            @Param("periodType") MissionType periodType,
            @Param("periodStart") LocalDate periodStart
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select mc
            from MissionCheck mc
            where mc.id = :missionCheckId
            """)
    Optional<MissionCheck> findByIdForUpdate(@Param("missionCheckId") Long missionCheckId);

    @Query("""
            select new com.fitpet.server.mission.domain.repository.MissionCheckKey(mc.mission.id, mc.user.id)
            from MissionCheck mc
            where mc.user.id in :userIds
              and mc.mission.id in :missionIds
              and mc.periodType = :periodType
              and mc.periodStart = :periodStart
            """)
    List<MissionCheckKey> findExistingKeys(
            @Param("userIds") List<Long> userIds,
            @Param("missionIds") List<Long> missionIds,
            @Param("periodType") MissionType periodType,
            @Param("periodStart") LocalDate periodStart
    );

    @Query("""
            select mc
            from MissionCheck mc
            where mc.user.id = :userId
            order by mc.periodStart desc
            """)
    List<MissionCheck> findRecentByUser(@Param("userId") Long userId);

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
