package com.fitpet.server.user.domain.repository;

import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.entity.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserAndDeviceUuid(User user, String deviceUuid);

    // 유저의 모든 기기 조회
    @Query("SELECT ud.deviceToken FROM UserDevice ud WHERE ud.user.id = :userId AND ud.deleted = false")
    List<String> findAllTokensByUserId(@Param("userId") Long userId);

    // 배치용
    @Query("SELECT ud.deviceToken FROM UserDevice ud WHERE ud.user IN :users AND ud.deleted = false")
    List<String> findAllTokensByUsers(@Param("users") List<User> users);
}