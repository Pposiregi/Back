package com.fitpet.server.user.application.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.auth.application.service.AuthService;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.termsmaster.application.service.TermsAgreementService;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.service.UserService;
import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserFacadeTest {

    @Mock UserService userService;
    @Mock TermsAgreementService termsAgreementService;
    @Mock UserRepository userRepository;
    @Mock AuthService authService;

    @InjectMocks UserFacade sut;

    private static final Long USER_ID = 1L;

    private User testUser() {
        return User.builder().id(USER_ID).email("test@test.com").build();
    }

    private UserInputInfoCommand dummyInfoCommand() {
        return new UserInputInfoCommand("닉네임", 25, Gender.male, 70.0, 175.0, 65.0, 20.0, 18.0, 8000);
    }

    private UserResult dummyUserResult() {
        return UserResult.builder().userId(USER_ID).build();
    }

    @Test
    void completeSignUp_존재하지_않는_userId면_BusinessException을_던진다() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.completeSignUp(999L, dummyInfoCommand(), List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void completeSignUp_약관_목록이_빈_리스트면_termsAgreementService가_호출된다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(userService.inputInfo(eq(USER_ID), any())).thenReturn(dummyUserResult());
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED))
                .when(termsAgreementService).saveTermsAgreements(eq(USER_ID), eq(Collections.emptyList()));

        assertThatThrownBy(() -> sut.completeSignUp(USER_ID, dummyInfoCommand(), Collections.emptyList()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void completeSignUp_약관_목록이_null이면_빈_리스트로_처리되어_termsAgreementService가_호출된다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(userService.inputInfo(eq(USER_ID), any())).thenReturn(dummyUserResult());
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.REQUIRED_TERMS_NOT_AGREED))
                .when(termsAgreementService).saveTermsAgreements(eq(USER_ID), eq(Collections.emptyList()));

        assertThatThrownBy(() -> sut.completeSignUp(USER_ID, dummyInfoCommand(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.REQUIRED_TERMS_NOT_AGREED.getMessage());
    }

    @Test
    void completeSignUp_약관_목록이_있으면_saveTermsAgreements가_호출된다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser()));
        when(userService.inputInfo(eq(USER_ID), any())).thenReturn(dummyUserResult());

        sut.completeSignUp(USER_ID, dummyInfoCommand(), List.of());

        verify(termsAgreementService).saveTermsAgreements(eq(USER_ID), any());
    }

    @Test
    void withdraw_호출_시_S3정리_후_DB처리_후_토큰_취소_순서대로_호출() {
        sut.withdraw(USER_ID);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(userService, authService);
        inOrder.verify(userService).cleanupUserImages(USER_ID);
        inOrder.verify(userService).withdrawUser(USER_ID);
        inOrder.verify(authService).revokeTokens(USER_ID);
    }

    @Test
    @DisplayName("withdraw 중 S3 삭제 예외 발생 시 DB 처리가 호출되지 않는다")
    void withdraw_S3_예외_시_DB_호출_안_됨() {
        doThrow(new RuntimeException("S3 error")).when(userService).cleanupUserImages(USER_ID);

        assertThatThrownBy(() -> sut.withdraw(USER_ID))
                .isInstanceOf(RuntimeException.class);

        verify(userService, never()).withdrawUser(USER_ID);
        verify(authService, never()).revokeTokens(USER_ID);
    }
}
