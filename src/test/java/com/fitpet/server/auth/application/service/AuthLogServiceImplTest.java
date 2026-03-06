package com.fitpet.server.auth.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fitpet.server.auth.application.dto.CreateAuthLogCommand;
import com.fitpet.server.auth.domain.entity.AuthLog;
import com.fitpet.server.auth.domain.entity.type.AuthEventType;
import com.fitpet.server.auth.domain.entity.type.AuthProvider;
import com.fitpet.server.auth.domain.repository.AuthLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthLogServiceImplTest {

    @Mock
    AuthLogRepository authLogRepository;

    @InjectMocks
    AuthLogServiceImpl sut;

    @Test
    void record_호출시_AuthLog가_저장된다() {
        CreateAuthLogCommand cmd = new CreateAuthLogCommand(
                1L, "test@test.com", AuthEventType.LOGIN, AuthProvider.LOCAL,
                "1.2.3.4", "Mozilla/5.0", true);

        sut.record(cmd);

        ArgumentCaptor<AuthLog> captor = ArgumentCaptor.forClass(AuthLog.class);
        verify(authLogRepository).save(captor.capture());
        AuthLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getEventType()).isEqualTo(AuthEventType.LOGIN);
        assertThat(saved.getAttemptedEmail()).isEqualTo("test@test.com");
    }

    @Test
    void 저장소_예외발생시_예외가_전파되지_않는다() {
        doThrow(new RuntimeException("DB 오류")).when(authLogRepository).save(any());
        CreateAuthLogCommand cmd = new CreateAuthLogCommand(
                null, "fail@test.com", AuthEventType.LOGIN, AuthProvider.LOCAL,
                "1.2.3.4", "Mozilla/5.0", false);

        assertThatCode(() -> sut.record(cmd)).doesNotThrowAnyException();
    }
}
