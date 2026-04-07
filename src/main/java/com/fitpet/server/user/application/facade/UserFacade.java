package com.fitpet.server.user.application.facade;

import com.fitpet.server.termsmaster.application.dto.TermsAgreementCommand;
import com.fitpet.server.termsmaster.application.service.TermsAgreementService;
import com.fitpet.server.user.application.dto.UserInputInfoCommand;
import com.fitpet.server.user.application.dto.UserResult;
import com.fitpet.server.user.application.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final TermsAgreementService termsAgreementService;

    @Transactional
    public UserResult completeSignUp(Long userId, UserInputInfoCommand infoCommand,
                                     List<TermsAgreementCommand> termsCommands) {
        UserResult result = userService.inputInfo(userId, infoCommand);

        if (termsCommands != null && !termsCommands.isEmpty()) {
            termsAgreementService.saveTermsAgreements(userId, termsCommands);
        }

        return result;
    }
}
