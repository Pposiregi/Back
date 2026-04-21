package com.fitpet.server.user.application.facade;

import com.fitpet.server.auth.application.service.AuthService;
import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import com.fitpet.server.termsmaster.application.service.TermsAgreementService;
import com.fitpet.server.shared.exception.BusinessException;
import com.fitpet.server.shared.exception.ErrorCode;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.service.UserService;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final TermsAgreementService termsAgreementService;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional
    public UserResult completeSignUp(Long userId, UserInputInfoCommand infoCommand,
                                     List<TermsAgreementCommand> termsCommands) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserResult result = userService.inputInfo(userId, infoCommand);

        termsAgreementService.saveTermsAgreements(userId,
                termsCommands != null ? termsCommands : List.of());

        return result;
    }

    public void withdraw(Long userId) {
        userService.withdrawUser(userId);
        authService.revokeTokens(userId);
    }
}
