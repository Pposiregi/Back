package com.fitpet.server.ranking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitpet.server.dailywalk.domain.repository.DailyWalkRepository;
import com.fitpet.server.ranking.application.dto.RankingDto;
import com.fitpet.server.ranking.domain.type.RankingFilter;
import com.fitpet.server.shared.s3.S3Service;
import com.fitpet.server.user.domain.entity.User;
import com.fitpet.server.user.domain.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    @Mock DailyWalkRepository dailyWalkRepository;
    @Mock UserRepository userRepository;
    @Mock S3Service s3Service;
    @Mock StringRedisTemplate redisTemplate;
    @Mock RedisScript<Long> updateRankingScript;
    @Mock RedisScript<Long> updateStepAndRankingScript;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock ZSetOperations<String, String> zSetOperations;
    @Mock HashOperations<String, Object, Object> hashOperations;

    @InjectMocks RankingServiceImpl sut;

    private static final Long USER_ID = 1L;
    private static final String USER_PROFILE_KEY = "user:profiles";
    private static final String USER_IMAGE_KEY = "user:images";
    private static final String RANKING_KEY = "ranking:daily:" + LocalDate.now();

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        Mockito.lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("getMyRank — Redis 캐시에 닉네임이 있으면 캐시의 닉네임을 반환한다")
    void getMyRank_Redis_캐시_닉네임_반환() {
        // given
        String cachedNickname = "캐시닉네임";
        String imageKey = "profile/1.jpg";
        String presignedUrl = "https://s3.example.com/profile/1.jpg";

        when(zSetOperations.reverseRank(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(0L);
        when(zSetOperations.score(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(5000.0);
        when(hashOperations.multiGet(eq(USER_PROFILE_KEY), anyList())).thenReturn(List.of(cachedNickname));
        when(hashOperations.multiGet(eq(USER_IMAGE_KEY), anyList())).thenReturn(List.of(imageKey));
        when(s3Service.generatePresignedGetUrl(imageKey)).thenReturn(presignedUrl);

        // when
        RankingDto result = sut.getMyRank(USER_ID, RankingFilter.ALL);

        // then
        assertThat(result.getNickname()).isEqualTo(cachedNickname);
        assertThat(result.getRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("getMyRank — Redis 캐시 미스 시 DB에서 닉네임을 조회해 반환한다")
    void getMyRank_캐시_미스_DB_닉네임_반환() {
        // given
        String dbNickname = "DB닉네임";
        User user = User.builder()
                .id(USER_ID)
                .nickname(dbNickname)
                .profileImageUrl("profile/1.jpg")
                .build();

        when(zSetOperations.reverseRank(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(0L);
        when(zSetOperations.score(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(5000.0);
        // 캐시 미스: nickname null, imageKey null
        when(hashOperations.multiGet(eq(USER_PROFILE_KEY), anyList())).thenReturn(singleNull());
        when(hashOperations.multiGet(eq(USER_IMAGE_KEY), anyList())).thenReturn(singleNull());
        when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(user));
        when(s3Service.generatePresignedGetUrl(any())).thenReturn("https://s3.example.com/profile/1.jpg");

        // when
        RankingDto result = sut.getMyRank(USER_ID, RankingFilter.ALL);

        // then
        assertThat(result.getNickname()).isEqualTo(dbNickname);
    }

    @Test
    @DisplayName("getMyRank — OAuth 초기 닉네임(이메일 prefix)이 캐시에 남아있어도 DB 갱신 후에는 DB 닉네임을 반환한다")
    void getMyRank_캐시_미스_후_DB_닉네임으로_교체된다() {
        // given — 캐시 미스 상황 (inputInfo 이후 캐시가 갱신됐다고 가정)
        String updatedNickname = "사용자설정닉네임";
        User userAfterInputInfo = User.builder()
                .id(USER_ID)
                .nickname(updatedNickname)
                .profileImageUrl(null)
                .build();

        when(zSetOperations.reverseRank(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(2L);
        when(zSetOperations.score(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(3000.0);
        when(hashOperations.multiGet(eq(USER_PROFILE_KEY), anyList())).thenReturn(singleNull());
        when(hashOperations.multiGet(eq(USER_IMAGE_KEY), anyList())).thenReturn(singleNull());
        when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(userAfterInputInfo));

        // when
        RankingDto result = sut.getMyRank(USER_ID, RankingFilter.ALL);

        // then
        assertThat(result.getNickname()).isEqualTo(updatedNickname);
        assertThat(result.getNickname()).doesNotContain("@"); // 이메일 prefix가 아님을 확인
    }

    @Test
    @DisplayName("getMyRank — 캐시 미스 시 userRepository.findAllById()를 호출해 DB에서 조회한다 (모니터링 대상)")
    void getMyRank_캐시_미스_시_DB_조회가_발생한다() {
        // given
        User user = User.builder().id(USER_ID).nickname("닉네임").profileImageUrl(null).build();

        when(zSetOperations.reverseRank(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(0L);
        when(zSetOperations.score(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(5000.0);
        when(hashOperations.multiGet(eq(USER_PROFILE_KEY), anyList())).thenReturn(singleNull());
        when(hashOperations.multiGet(eq(USER_IMAGE_KEY), anyList())).thenReturn(singleNull());
        when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(user));

        // when
        sut.getMyRank(USER_ID, RankingFilter.ALL);

        // then — 캐시 미스이므로 DB 조회가 발생해야 한다
        verify(userRepository).findAllById(List.of(USER_ID));
    }

    @Test
    @DisplayName("getMyRank — 캐시 히트 시 userRepository.findAllById()를 호출하지 않는다")
    void getMyRank_캐시_히트_시_DB_조회가_발생하지_않는다() {
        // given
        when(zSetOperations.reverseRank(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(0L);
        when(zSetOperations.score(eq(RANKING_KEY), eq(String.valueOf(USER_ID)))).thenReturn(5000.0);
        when(hashOperations.multiGet(eq(USER_PROFILE_KEY), anyList())).thenReturn(List.of("캐시닉네임"));
        when(hashOperations.multiGet(eq(USER_IMAGE_KEY), anyList())).thenReturn(List.of("profile/1.jpg"));
        when(s3Service.generatePresignedGetUrl(any())).thenReturn("https://s3.example.com/1.jpg");

        // when
        sut.getMyRank(USER_ID, RankingFilter.ALL);

        // then — 캐시 히트이므로 DB 조회가 발생하지 않아야 한다
        verify(userRepository, never()).findAllById(anyList());
    }

    /** List.of()는 null 원소를 허용하지 않으므로 별도 헬퍼 사용 */
    private List<Object> singleNull() {
        List<Object> list = new java.util.ArrayList<>();
        list.add(null);
        return list;
    }
}
