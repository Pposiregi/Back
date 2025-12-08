package com.fitpet.server.user.presentation.controller;

<<<<<<< HEAD
import com.fitpet.server.shared.annotation.AuthUser;
=======
import com.fitpet.server.security.jwt.JwtTokenProvider;
import com.fitpet.server.user.application.dto.GenderRankingResult;
import com.fitpet.server.user.application.dto.RankingResult;
import com.fitpet.server.user.application.dto.UserRanking;
>>>>>>> 6b517bddf87b0b81fa5cd46531ab7e9faad20407
import com.fitpet.server.user.application.service.UserService;
import com.fitpet.server.user.domain.entity.Gender;
import com.fitpet.server.user.presentation.dto.GenderRankingResponse;
import com.fitpet.server.user.presentation.dto.RankingResponse;
import com.fitpet.server.user.presentation.dto.UserCreateRequest;
import com.fitpet.server.user.presentation.dto.UserDto;
import com.fitpet.server.user.presentation.dto.UserInputInfoRequest;
import com.fitpet.server.user.presentation.dto.UserRankingDto;
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
import org.springframework.web.bind.annotation.RequestParam;
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
<<<<<<< HEAD
            @AuthUser Long userId
=======
            @PathVariable Long userId
>>>>>>> 6b517bddf87b0b81fa5cd46531ab7e9faad20407
    ) {
        log.info("[UserController] 사용자 조회 요청: id: {}", userId);

        UserDto user = userService.findUser(userId);

        log.info("[UserController] 사용자 조회 완료: id: {}", userId);

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @PatchMapping
    public ResponseEntity<UserDto> update(
<<<<<<< HEAD
            @AuthUser Long userId,
=======
            @PathVariable Long userId,
>>>>>>> 6b517bddf87b0b81fa5cd46531ab7e9faad20407
            @Valid @RequestBody UserUpdateRequest userUpdateRequest
    ) {
        log.info("[UserController] 사용자 수정 요청: id: {}", userId);

        UserDto user = userService.updateUser(userId, userUpdateRequest);

        log.info("[UserController] 사용자 수정 완료: id: {}", userId);

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @GetMapping("/rankings/daily-step")
    public ResponseEntity<RankingResponse> getDailyStepRanking(
            @RequestParam(defaultValue = "10") int limit
    ) {
        // TODO: 추후 토큰에서 userId 추출 로직으로 교체
        Long userId = 3L;
        log.info("[UserController] 일일 걸음 랭킹 조회 요청: userId={}, limit={}", userId, limit);
        RankingResult rankingResult = userService.getDailyStepRanking(userId, limit);
        RankingResponse rankingResponse = RankingResponse.from(rankingResult);
        log.info("[UserController] 일일 걸음 랭킹 조회 완료: userId={}, limit={}", userId, limit);
        return ResponseEntity.ok(rankingResponse);
    }

    @GetMapping("/rankings/daily-step/gender")
    public ResponseEntity<GenderRankingResponse> getGenderDailyStepRanking(
            @RequestParam Gender gender,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("[UserController] 성별 일일 걸음 랭킹 조회 요청: gender={}, limit={}", gender, limit);
        GenderRankingResult result = userService.getGenderDailyStepRanking(gender, limit);
        GenderRankingResponse response = GenderRankingResponse.from(result);
        log.info("[UserController] 성별 일일 걸음 랭킹 조회 완료: gender={}, limit={}", gender, limit);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/signUp/complete")
    public ResponseEntity<UserDto> updateUserInfo(
<<<<<<< HEAD
            @AuthUser Long userId,
=======
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
>>>>>>> 6b517bddf87b0b81fa5cd46531ab7e9faad20407
            @Valid @RequestBody UserInputInfoRequest userInputInfoRequest
    ) {
        log.info("[UserController] 현재 사용자 정보 입력 요청: id: {}", userId);

        UserDto user = userService.inputInfo(userId, userInputInfoRequest);

        log.info("[UserController] 현재 사용자 정보 입력 완료: id: {}", userId);

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
<<<<<<< HEAD
            @AuthUser Long userId
=======
            @PathVariable Long userId
>>>>>>> 6b517bddf87b0b81fa5cd46531ab7e9faad20407
    ) {
        log.info("[UserController] 사용자 삭제 요청: id: {}", userId);

        userService.deleteUser(userId);

        log.info("[UserController] 사용자 삭제 완료: id: {}", userId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

<<<<<<< HEAD
}
=======
    private static String extractBearerToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        int space = authHeader.indexOf(' ');
        if (space < 0) {
            return null;
        }
        String scheme = authHeader.substring(0, space);
        if (!"Bearer".equalsIgnoreCase(scheme)) {
            return null;
        }
        String token = authHeader.substring(space + 1).trim();
        return token.isEmpty() ? null : token;
    }

}
>>>>>>> 6b517bddf87b0b81fa5cd46531ab7e9faad20407
