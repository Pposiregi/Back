package com.fitpet.server.user.presentation.controller;

import com.fitpet.server.shared.annotation.AuthUser;
import com.fitpet.server.user.application.service.UserService;
import com.fitpet.server.user.presentation.dto.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.UserDto;
import com.fitpet.server.user.presentation.dto.UserInputInfoRequest;
import com.fitpet.server.user.presentation.dto.UserUpdateRequest;
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
    public ResponseEntity<UserDto> create(
            @Valid @RequestBody UserCreateRequest userCreateRequest
    ) {
        log.info("[UserController] 사용자 회원가입 요청: email: {}, nickname: {}",
                userCreateRequest.email(), userCreateRequest.nickname());

        UserDto createdUser = userService.createUser(userCreateRequest);

        log.info("[UserController] 사용자 회원가입 완료: id: {}, email: {}",
                createdUser.userId(), createdUser.email());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdUser);
    }

    @GetMapping
    public ResponseEntity<UserDto> find(
            @AuthUser Long userId
    ) {
        log.info("[UserController] 사용자 조회 요청: id: {}", userId);

        UserDto user = userService.findUser(userId);

        log.info("[UserController] 사용자 조회 완료: id: {}", userId);

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @PatchMapping
    public ResponseEntity<UserDto> update(
            @AuthUser Long userId,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest
    ) {
        log.info("[UserController] 사용자 수정 요청: id: {}", userId);

        UserDto user = userService.updateUser(userId, userUpdateRequest);

        log.info("[UserController] 사용자 수정 완료: id: {}", userId);

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @PatchMapping("/signUp/complete")
    public ResponseEntity<UserDto> updateUserInfo(
            @AuthUser Long userId,
            @Valid @RequestBody UserInputInfoRequest userInputInfoRequest
    ) {
        log.info("[UserController] 현재 사용자 정보 입력 요청: id: {}", userId);

        // 이미 가입 완료된 유저인지 확인하는 로직 (Service로 옮기는 것도 고려해볼 만함)
        if (userService.isRegistrationComplete(userId)) {
            // 이미 완료된 경우 400 Bad Request 또는 409 Conflict가 더 적절할 수 있으나, 기존 유지 시 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        UserDto user = userService.inputInfo(userId, userInputInfoRequest);

        log.info("[UserController] 현재 사용자 정보 입력 완료: id: {}", userId);

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @AuthUser Long userId
    ) {
        log.info("[UserController] 사용자 삭제 요청: id: {}", userId);

        userService.deleteUser(userId);

        log.info("[UserController] 사용자 삭제 완료: id: {}", userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}