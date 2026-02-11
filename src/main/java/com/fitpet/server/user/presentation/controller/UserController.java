package com.fitpet.server.user.presentation.controller;

import com.fitpet.server.shared.annotation.AuthUser;
import com.fitpet.server.user.application.service.UserService;
import com.fitpet.server.user.presentation.dto.UserDto;
import com.fitpet.server.user.presentation.dto.request.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.request.UserInputInfoRequest;
import com.fitpet.server.user.presentation.dto.request.UserUpdateRequest;
import com.fitpet.server.user.presentation.dto.response.ProfileImageUpdateResponse;
import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest request) {
        UserDto createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping
    public ResponseEntity<UserDto> find(@AuthUser Long userId) {
        return ResponseEntity.ok(userService.findUser(userId));
    }

    @PatchMapping
    public ResponseEntity<UserDto> update(@AuthUser Long userId, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    // [수정] 프로필 이미지 변경 요청 (Presigned URL 발급) - 기존 랭킹 API는 제거됨
    @PostMapping("/profile-image")
    public ResponseEntity<ProfileImageUpdateResponse> updateProfileImage(@AuthUser Long userId) {
        log.info("[UserController] 프로필 이미지 변경 요청: id: {}", userId);
        ProfileImageUpdateResponse response = userService.updateProfileImage(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/signUp/complete")
    public ResponseEntity<UserDto> updateUserInfo(@AuthUser Long userId,
                                                  @Valid @RequestBody UserInputInfoRequest request) {
        return ResponseEntity.ok(userService.inputInfo(userId, request));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthUser Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}