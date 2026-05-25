package com.fitpet.server.shared.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fitpet.server.shared.notification.ErrorContext;
import com.fitpet.server.shared.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock NotificationService notificationService;
    @InjectMocks GlobalExceptionHandler sut;

    @Test
    @DisplayName("처리되지 않은 Exception 발생 시 notifyError가 호출된다")
    void handleUnexpected_notifyError_호출() {
        RuntimeException e = new RuntimeException("unexpected");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");

        sut.handleUnexpected(e, request);

        verify(notificationService).notifyError(eq(e), any(ErrorContext.class));
    }

    @Test
    @DisplayName("BusinessException이 5xx이면 notifyError가 호출된다")
    void handleBusinessException_5xx이면_notifyError_호출() {
        BusinessException e = new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

        sut.handleBusinessException(e, request);

        verify(notificationService).notifyError(eq(e), any(ErrorContext.class));
    }

    @Test
    @DisplayName("BusinessException이 4xx이면 notifyError가 호출되지 않는다")
    void handleBusinessException_4xx이면_notifyError_미호출() {
        BusinessException e = new BusinessException(ErrorCode.USER_NOT_FOUND);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/1");

        sut.handleBusinessException(e, request);

        verify(notificationService, never()).notifyError(any(), any());
    }
}
