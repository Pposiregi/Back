package com.fitpet.server.auth.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitpet.server.auth.application.dto.CreateAuthLogCommand;
import com.fitpet.server.auth.application.service.AuthLogService;
import com.fitpet.server.auth.domain.entity.type.AuthEventType;
import com.fitpet.server.auth.domain.entity.type.AuthProvider;
import com.fitpet.server.shared.config.FcmConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * 실제 DB(MySQL)에 auth_log가 저장되는지 검증하는 통합 테스트.
 * - @Async 비동기 저장이므로 500ms 대기 후 DB 조회
 * - 실행 전 auth_log 테이블이 DB에 존재해야 함 (ddl-auto: update로 자동 생성)
 */
@SpringBootTest
@ActiveProfiles("local")
class AuthLogIntegrationTest {

    @MockBean
    private FcmConfig fcmConfig;

    @MockBean
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private AuthLogService authLogService;

    @Autowired
    private AuthLogJpaRepository authLogJpaRepository;

    @Test
    @DisplayName("카카오 로그인 성공 시 auth_log DB에 저장된다")
    void 카카오_로그인_성공_로그_DB_저장() throws InterruptedException {
        // given
        long beforeCount = authLogJpaRepository.count();
        CreateAuthLogCommand command = new CreateAuthLogCommand(
                1L,
                null,
                AuthEventType.LOGIN,
                AuthProvider.KAKAO,
                "1.2.3.4",
                "PostmanRuntime/7.0",
                true
        );

        // when
        authLogService.record(command);
        Thread.sleep(500); // @Async 비동기 완료 대기

        // then
        long afterCount = authLogJpaRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount + 1);

        List<com.fitpet.server.auth.domain.entity.AuthLog> logs = authLogJpaRepository.findAll();
        com.fitpet.server.auth.domain.entity.AuthLog last = logs.get(logs.size() - 1);
        assertThat(last.getEventType()).isEqualTo(AuthEventType.LOGIN);
        assertThat(last.getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(last.isSuccess()).isTrue();
        assertThat(last.getUserId()).isEqualTo(1L);
        assertThat(last.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("구글 로그인 성공 시 auth_log DB에 저장된다")
    void 구글_로그인_성공_로그_DB_저장() throws InterruptedException {
        // given
        long beforeCount = authLogJpaRepository.count();
        CreateAuthLogCommand command = new CreateAuthLogCommand(
                2L,
                null,
                AuthEventType.LOGIN,
                AuthProvider.GOOGLE,
                "5.6.7.8",
                "Mozilla/5.0",
                true
        );

        // when
        authLogService.record(command);
        Thread.sleep(500);

        // then
        long afterCount = authLogJpaRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount + 1);

        List<com.fitpet.server.auth.domain.entity.AuthLog> logs = authLogJpaRepository.findAll();
        com.fitpet.server.auth.domain.entity.AuthLog last = logs.get(logs.size() - 1);
        assertThat(last.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(last.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("로그아웃 시 LOGOUT 이벤트가 DB에 저장된다")
    void 로그아웃_로그_DB_저장() throws InterruptedException {
        // given
        long beforeCount = authLogJpaRepository.count();
        CreateAuthLogCommand command = new CreateAuthLogCommand(
                1L,
                null,
                AuthEventType.LOGOUT,
                AuthProvider.LOCAL,
                "1.2.3.4",
                "PostmanRuntime/7.0",
                true
        );

        // when
        authLogService.record(command);
        Thread.sleep(500);

        // then
        long afterCount = authLogJpaRepository.count();
        assertThat(afterCount).isEqualTo(beforeCount + 1);

        List<com.fitpet.server.auth.domain.entity.AuthLog> logs = authLogJpaRepository.findAll();
        com.fitpet.server.auth.domain.entity.AuthLog last = logs.get(logs.size() - 1);
        assertThat(last.getEventType()).isEqualTo(AuthEventType.LOGOUT);
        assertThat(last.isSuccess()).isTrue();
    }
}
