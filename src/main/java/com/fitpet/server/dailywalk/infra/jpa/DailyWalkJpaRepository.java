package com.fitpet.server.dailywalk.infra.jpa;

import com.fitpet.server.dailywalk.domain.entity.DailyWalk;
import com.fitpet.server.user.domain.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyWalkJpaRepository extends JpaRepository<DailyWalk, Long> {
    List<DailyWalk> findAllByUser_Id(Long userId);

    @Query("""
            select dw
            from DailyWalk dw
            where dw.user.id = :userId
              and dw.createdAt >= :start
              and dw.createdAt < :end
            """)
    Optional<DailyWalk> findByPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
            select dw
            from DailyWalk dw
            where dw.user.id = :userId
              and dw.createdAt >= :start
              and dw.createdAt < :end
            order by dw.createdAt asc
            """)
    List<DailyWalk> findAllByPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT dw FROM DailyWalk dw " +
            "WHERE dw.user = :user " +
            "AND FUNCTION('DATE', dw.createdAt) BETWEEN :start AND :end")
    List<DailyWalk> findByUserAndDateBetween(
            @Param("user") User user,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    boolean existsById(Long id);

    void deleteById(Long id);

    @Query("""
            select dw from DailyWalk dw
            join fetch dw.user
            where dw.createdAt >= :start
              and dw.createdAt < :end
            """)
    List<DailyWalk> findAllByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DailyWalk dw
               set dw.step = :step,
                   dw.distanceKm = :distanceKm,
                   dw.burnCalories = :burnCalories
            where dw.user.id = :userId
               and dw.createdAt >= :start
               and dw.createdAt < :end
            """)
    int updateStepByUserIdAndDate(@Param("userId") Long userId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("step") Integer step,
                                  @Param("distanceKm") BigDecimal distanceKm,
                                  @Param("burnCalories") Integer burnCalories);

}
