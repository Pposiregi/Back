package com.fitpet.server.user.infra.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitpet.server.user.domain.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class UserSoftDeleteRepositoryTest {

    @Autowired
    UserJpaRepository userJpaRepository;

    @Autowired
    TestEntityManager em;

    // ──────────────────────────────────────────────
    // @SQLRestriction 동작 검증
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("탈퇴한 계정은 findByEmail로 조회되지 않는다 (@SQLRestriction)")
    void findByEmail_탈퇴_계정_조회_안_됨() {
        User user = savedUser("soft@test.com", null, null);
        user.withdraw();
        userJpaRepository.saveAndFlush(user);
        em.clear();

        Optional<User> result = userJpaRepository.findByEmail("soft@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("탈퇴한 계정은 findById로 조회되지 않는다 (@SQLRestriction)")
    void findById_탈퇴_계정_조회_안_됨() {
        User user = savedUser("byid@test.com", null, null);
        user.withdraw();
        userJpaRepository.saveAndFlush(user);
        Long id = user.getId();
        em.clear();

        Optional<User> result = userJpaRepository.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("활성 계정은 findByEmail로 정상 조회된다")
    void findByEmail_활성_계정_정상_조회() {
        savedUser("active@test.com", null, null);
        em.clear();

        Optional<User> result = userJpaRepository.findByEmail("active@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getDeletedAt()).isNull();
    }

    // ──────────────────────────────────────────────
    // native query — @SQLRestriction 우회 검증
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("탈퇴한 계정은 findByEmailIncludeDeleted로 조회된다 (native query)")
    void findByEmailIncludeDeleted_탈퇴_계정_조회_됨() {
        User user = savedUser("deleted@test.com", null, null);
        user.withdraw();
        userJpaRepository.saveAndFlush(user);
        em.clear();

        Optional<User> result = userJpaRepository.findByEmailIncludeDeleted("deleted@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("탈퇴한 소셜 계정은 findByOAuthIncludeDeleted로 조회된다 (native query)")
    void findByOAuthIncludeDeleted_탈퇴_소셜_계정_조회_됨() {
        User user = savedUser("kakao@test.com", "KAKAO", "kakao-uid-123");
        user.withdraw();
        userJpaRepository.saveAndFlush(user);
        em.clear();

        Optional<User> result = userJpaRepository.findByOAuthIncludeDeleted("KAKAO", "kakao-uid-123");

        assertThat(result).isPresent();
        assertThat(result.get().getDeletedAt()).isNotNull();
        assertThat(result.get().getProvider()).isEqualTo("KAKAO");
        assertThat(result.get().getProviderUid()).isEqualTo("kakao-uid-123");
    }

    @Test
    @DisplayName("활성 소셜 계정도 findByOAuthIncludeDeleted로 조회된다")
    void findByOAuthIncludeDeleted_활성_소셜_계정_조회_됨() {
        savedUser("google@test.com", "GOOGLE", "google-uid-456");
        em.clear();

        Optional<User> result = userJpaRepository.findByOAuthIncludeDeleted("GOOGLE", "google-uid-456");

        assertThat(result).isPresent();
        assertThat(result.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("findByEmailIncludeDeleted는 존재하지 않는 이메일이면 empty를 반환한다")
    void findByEmailIncludeDeleted_없는_이메일이면_empty() {
        Optional<User> result = userJpaRepository.findByEmailIncludeDeleted("none@test.com");

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────
    // helper
    // ──────────────────────────────────────────────

    private User savedUser(String email, String provider, String providerUid) {
        User user = User.builder()
                .email(email)
                .password("encoded-pw")
                .provider(provider)
                .providerUid(providerUid)
                .build();
        return userJpaRepository.saveAndFlush(user);
    }
}
