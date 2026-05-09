package com.fitpet.server.user.presentation.controller;

import com.fitpet.server.shared.annotation.AuthUser;
import com.fitpet.server.user.application.facade.UserFacade;
import com.fitpet.server.user.application.service.UserService;
import com.fitpet.server.user.presentation.dto.UserDto;
import com.fitpet.server.user.presentation.dto.request.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.request.UserInputInfoRequest;
import com.fitpet.server.user.presentation.dto.request.UserUpdateRequest;
import com.fitpet.server.user.presentation.dto.response.ProfileImageHistoryResponse;
import com.fitpet.server.user.presentation.dto.response.ProfileImageUpdateResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserFacade userFacade;

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest userCreateRequest) {
        UserDto createdUser = UserDto.from(userService.createUser(userCreateRequest.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping
    public ResponseEntity<UserDto> find(@AuthUser Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(UserDto.from(userService.findUser(userId)));
    }

    @PatchMapping
    public ResponseEntity<UserDto> update(@AuthUser Long userId,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        // S3 검증은 트랜잭션 밖에서 먼저 수행
        if (StringUtils.hasText(userUpdateRequest.profileImageKey())) {
            userService.checkHistoryImageAccess(userId, userUpdateRequest.profileImageKey());
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(UserDto.from(userService.updateUser(userId, userUpdateRequest.toCommand())));
    }

    @PostMapping("/profile-image")
    public ResponseEntity<ProfileImageUpdateResponse> updateProfileImage(@AuthUser Long userId) {
        return ResponseEntity.ok(ProfileImageUpdateResponse.from(userService.updateProfileImage(userId)));
    }

    @PatchMapping("/signUp/complete")
    public ResponseEntity<UserDto> updateUserInfo(@AuthUser Long userId,
            @Valid @RequestBody UserInputInfoRequest request) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(UserDto.from(userFacade.completeSignUp(userId, request.toCommand(), request.toTermsCommands())));
    }

    @GetMapping("/profile-image/history")
    public ResponseEntity<List<ProfileImageHistoryResponse>> getProfileImageHistory(@AuthUser Long userId) {
        List<ProfileImageHistoryResponse> history = userService.getProfileImageHistory(userId)
                .stream()
                .map(ProfileImageHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/profile-image")
    public ResponseEntity<Void> deleteProfileImage(@AuthUser Long userId) {
        userService.deleteProfileImage(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthUser Long userId) {
        userFacade.withdraw(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
