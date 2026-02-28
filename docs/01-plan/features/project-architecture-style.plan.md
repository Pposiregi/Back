# Plan: 프로젝트 아키텍처 및 개발 스타일 가이드

## 1. 개요

| 항목 | 내용 |
|------|------|
| **기능명** | 프로젝트 개발 스타일 기준서 |
| **작성일** | 2026-02-28 |
| **작성자** | Claude Code (PDCA Plan) |
| **브랜치** | refactor/ranking |
| **프레임워크** | Spring Boot 3.3.0, Java 17 |

---

## 2. 현재 프로젝트 아키텍처 분석

### 2.1 전체 구조: 헥사고날 + 레이어드 아키텍처

프로젝트는 **헥사고날 아키텍처(Ports & Adapters)**를 기반으로 **도메인 주도 설계(DDD)** 방식을 채택하고 있습니다.

```
src/main/java/com/fitpet/
├── {module}/
│   ├── presentation/           ← HTTP 레이어 (입출력 경계)
│   │   ├── controller/
│   │   └── dto/
│   │       ├── request/
│   │       └── response/
│   ├── application/            ← 비즈니스 유스케이스 레이어
│   │   ├── service/
│   │   ├── mapper/
│   │   ├── dto/                ← Command, Result, ApplicationDto
│   │   ├── facade/             ← 복잡한 서비스 조합 시만 사용
│   │   ├── scheduler/
│   │   └── event/
│   ├── domain/                 ← 핵심 도메인 (프레임워크 독립)
│   │   ├── entity/
│   │   ├── repository/         ← 인터페이스만 정의 (Port)
│   │   ├── exception/
│   │   └── type/               ← Enum, Value Objects
│   └── infra/                  ← 기술 구현체 (Adapter)
│       ├── jpa/                ← JpaRepository + 어댑터
│       ├── adapter/
│       └── scheduler/
└── shared/                     ← 공통 인프라
    ├── config/
    ├── exception/
    ├── security/
    ├── s3/
    ├── annotation/
    ├── resolver/
    └── util/
```

### 2.2 모듈 목록 (15개)

| 모듈 | 역할 |
|------|------|
| `auth` | JWT 인증, OAuth (Google/Kakao) |
| `user` | 사용자 프로필, 디바이스, 이미지 |
| `pet` | 반려동물 관리 |
| `ranking` | Redis 기반 실시간 랭킹 |
| `mission` | 미션/목표 관리 |
| `badge` | 배지 시스템 |
| `meal` | 식사 기록 |
| `bodyhistory` | 체중/신체 이력 |
| `dailywalk` | 일일 걸음 추적 |
| `dailyworkout` | GPS 운동 세션 |
| `alram` | 알림 관리 |
| `report` | 리포트/분석 |
| `termsmaster` | 약관 관리 |
| `security` | JWT 필터/처리 |
| `shared` | 공통 인프라 |

---

## 3. 레이어별 역할 및 규칙

### 3.1 Presentation Layer (HTTP 경계)

**역할**: HTTP 요청 수신 → 유효성 검사 → Application 레이어 위임 → HTTP 응답 반환

**Controller 규칙:**
```java
@RestController
@RequestMapping("/api/{module}")
@RequiredArgsConstructor
@Tag(name = "...", description = "...")
public class {Entity}Controller {

    private final {Entity}Service {entity}Service;

    @GetMapping("/{id}")
    public ResponseEntity<{Entity}Response> find(@AuthUser Long userId, @PathVariable Long id) {
        return ResponseEntity.ok({entity}Service.find(userId, id));
    }

    @PostMapping
    public ResponseEntity<{Entity}Response> create(@AuthUser Long userId,
                                                   @Valid @RequestBody {Entity}CreateRequest request) {
        return ResponseEntity.status(CREATED).body({entity}Service.create(userId, request));
    }
}
```

**HTTP 상태코드 기준:**
- `200 OK` → GET, PATCH (수정 후 반환)
- `201 CREATED` → POST (생성)
- `204 NO_CONTENT` → DELETE

**Request DTO 규칙 (Record 사용):**
```java
public record {Entity}CreateRequest(
    @NotBlank String field1,
    @Min(1) Integer field2
) {}
```

**Response DTO 규칙:**
- 단순 응답: `record {Entity}Response(...) {}`
- 복잡한 응답 (빌더/정적 팩토리): `@Getter @Builder class {Entity}Response`

### 3.2 Application Layer (비즈니스 로직)

**역할**: 유스케이스 구현, 트랜잭션 경계 관리, 도메인 간 조합

**Service 인터페이스 / 구현체 분리:**
```java
// 인터페이스 (domain 계약 정의)
public interface {Entity}Service {
    {Entity}Dto find(Long userId, Long id);
    {Entity}Dto create(Long userId, {Entity}CreateRequest request);
    void delete(Long userId, Long id);
}

// 구현체 (@Service, @Transactional)
@Service
@RequiredArgsConstructor
public class {Entity}ServiceImpl implements {Entity}Service {

    private final {Entity}Repository {entity}Repository;
    private final {Entity}Mapper {entity}Mapper;

    @Override
    @Transactional(readOnly = true)
    public {Entity}Dto find(Long userId, Long id) {
        {Entity} entity = find{Entity}ById(id);
        return {entity}Mapper.toDto(entity);
    }

    // Private 헬퍼 메서드
    private {Entity} find{Entity}ById(Long id) {
        return {entity}Repository.findById(id)
                .orElseThrow(() -> new {Entity}NotFoundException(ErrorCode.{ENTITY}_NOT_FOUND));
    }
}
```

**Facade (복잡한 조합 시에만 사용):**
```java
@Component
@RequiredArgsConstructor
public class {Entity}Facade {
    private final ServiceA serviceA;
    private final ServiceB serviceB;
    // 여러 서비스를 조합해 복잡한 결과 생성
}
```

**Application DTO:**
- `{Entity}CreateCommand` → 입력 명령 객체 (record)
- `{Entity}Result` / `{Entity}Dto` → 출력 결과 객체

### 3.3 Domain Layer (핵심 도메인)

**역할**: 비즈니스 규칙, 도메인 모델, 리포지토리 인터페이스 (프레임워크 독립)

**Entity 규칙:**
```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@DynamicUpdate
@Table(name = "...", indexes = @Index(name = "...", columnList = "..."))
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class {Entity} {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 비즈니스 메서드 (도메인 행위)
    public void update(String field) {
        this.field = field;
    }

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

**Repository 인터페이스 (Port):**
```java
public interface {Entity}Repository {
    Optional<{Entity}> findById(Long id);
    {Entity} save({Entity} entity);
    void delete({Entity} entity);
    List<{Entity}> findByUserId(Long userId);
}
```

**도메인 예외:**
```java
public class {Entity}NotFoundException extends BusinessException {
    public {Entity}NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

### 3.4 Infrastructure Layer (기술 구현체)

**역할**: 도메인 인터페이스 구현, JPA/Redis/외부 API 연동

**Repository Adapter:**
```java
@Repository
@RequiredArgsConstructor
public class {Entity}RepositoryAdapter implements {Entity}Repository {

    private final {Entity}JpaRepository {entity}JpaRepository;

    @Override
    public Optional<{Entity}> findById(Long id) {
        return {entity}JpaRepository.findById(id);
    }
}
```

**JPA Repository:**
```java
public interface {Entity}JpaRepository extends JpaRepository<{Entity}, Long> {
    Optional<{Entity}> findByUserId(Long userId);
    // JPQL이 필요한 경우만 @Query 사용
}
```

---

## 4. 네이밍 컨벤션

### 4.1 클래스 네이밍

| 용도 | 패턴 | 예시 |
|------|------|------|
| 서비스 인터페이스 | `{Entity}Service` | `UserService` |
| 서비스 구현체 | `{Entity}ServiceImpl` | `UserServiceImpl` |
| 복합 조합 | `{Entity}Facade` | `RankingFacade` |
| 매퍼 | `{Entity}Mapper` | `UserMapper` |
| 컨트롤러 | `{Entity}Controller` | `UserController` |
| JPA 리포지토리 | `{Entity}JpaRepository` | `UserJpaRepository` |
| 리포지토리 어댑터 | `{Entity}RepositoryAdapter` | `UserRepositoryAdapter` |
| 도메인 예외 | `{Entity}{Reason}Exception` | `UserNotFoundException` |
| 요청 DTO | `{Entity}CreateRequest`, `{Entity}UpdateRequest` | `PetCreateRequest` |
| 응답 DTO | `{Entity}Response`, `{Entity}Dto` | `RankingResponse` |
| 커맨드 DTO | `{Entity}CreateCommand`, `{Entity}UpdateCommand` | `MealCreateCommand` |
| 결과 DTO | `{Entity}Result`, `{Entity}Dto` | `MissionResult` |
| Enum/타입 | `{Concept}Type`, `{Concept}Filter` | `RankingFilter`, `MealTime` |

### 4.2 메서드 네이밍

| 동작 | 접두어 | 예시 |
|------|--------|------|
| 조회 | `find*`, `get*` | `findUser()`, `getMyRank()` |
| 생성 | `create*` | `createUser()` |
| 수정 | `update*` | `updateScore()` |
| 삭제 | `delete*` | `deletePet()` |
| 불린 검사 | `is*`, `has*` | `isRegistrationComplete()` |
| 계산 | `calculate*` | `calculateTimeWeightedScore()` |
| **Private 헬퍼** | | |
| 조회 헬퍼 | `find*By*()` | `findUserById()`, `findPetByUserId()` |
| 검증 | `validate*()` | `validateUserCreateRequest()` |
| 권한 확인 | `authorize*()` | `authorizeMealOwner()` |
| 데이터 보강 | `enrichWith*()` | `enrichWithPresignedUrl()` |
| 변환 | `convert*()` | `convertToResponseList()` |

---

## 5. 공통 인프라 패턴

### 5.1 인증/인가
- `@AuthUser` 커스텀 어노테이션으로 현재 사용자 ID 주입
- `AuthUserArgumentResolver`가 SecurityContext에서 userId 추출
- Controller 메서드: `(@AuthUser Long userId, ...)`

### 5.2 예외 처리
```
BusinessException (기반)
└── {Entity}NotFoundException
└── {Entity}AccessDeniedException
└── Duplicate{Entity}Exception

GlobalExceptionHandler (@RestControllerAdvice)
├── BusinessException → ErrorCode 기반 응답
├── MethodArgumentNotValidException → 필드 검증 오류
└── ConstraintViolationException → 경로변수 검증 오류

에러 응답 형식:
{
    "code": "U001",
    "message": "사용자를 찾을 수 없습니다."
}
```

### 5.3 S3 Presigned URL
- `S3Service.generatePresignedPutUrl()` → 이미지 업로드 (10분)
- `S3Service.generatePresignedGetUrl()` → 이미지 조회 (60분)
- 이미지 키 형식: `user/{userId}/{imageType}/{UUID}.jpg`

### 5.4 Redis 캐싱
```
user:profiles  → Hash{userId → nickname}
user:images    → Hash{userId → imageKey}
user:genders   → Hash{userId → gender}
ranking:{filter} → ZSet{userId → score}
```

### 5.5 MapStruct 매퍼
```java
@Mapper(componentModel = "spring")
public interface {Entity}Mapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    {Entity} toEntity({Entity}CreateRequest request);

    @Mapping(source = "id", target = "{entity}Id")
    {Entity}Dto toDto({Entity} entity);
}
```

### 5.6 트랜잭션 전략
- 서비스 구현체 클래스 수준: `@Transactional`
- 읽기 전용 메서드: `@Transactional(readOnly = true)`
- 쓰기 메서드: 클래스 레벨 `@Transactional` 상속

---

## 6. 신규 기능 개발 체크리스트

새로운 기능을 추가할 때 이 프로젝트의 스타일에 맞게 개발하는 순서:

### ① Domain Layer 먼저
- [ ] Entity 클래스 (`@DynamicUpdate`, `@Builder`, 도메인 메서드 포함)
- [ ] Repository 인터페이스 (Port 정의)
- [ ] 도메인 예외 클래스들
- [ ] Enum/타입 (`domain/type/`)

### ② Infrastructure Layer
- [ ] JpaRepository 인터페이스
- [ ] RepositoryAdapter (도메인 인터페이스 구현)
- [ ] Redis, S3 등 외부 연동 구현체

### ③ Application Layer
- [ ] 커맨드/결과 DTO (record)
- [ ] MapStruct Mapper 인터페이스
- [ ] Service 인터페이스 정의
- [ ] ServiceImpl 구현 (`@Service`, `@Transactional`)
- [ ] Facade (여러 서비스 조합 필요 시)

### ④ Presentation Layer
- [ ] Request DTO (record + Bean Validation)
- [ ] Response DTO (record 또는 @Builder class)
- [ ] Controller (`@RestController`, `@AuthUser` 적용)
- [ ] Swagger 어노테이션 (`@Tag`, `@Operation`)

### ⑤ 공통
- [ ] ErrorCode enum에 새 에러 코드 추가
- [ ] 도메인 예외 등록

---

## 7. 금지 사항 (안티패턴)

| 안티패턴 | 올바른 방법 |
|---------|------------|
| Controller에서 비즈니스 로직 | Service로 위임 |
| Entity를 Controller에서 직접 반환 | DTO로 변환 후 반환 |
| JpaRepository를 Service에서 직접 주입 | RepositoryAdapter → 도메인 Repository 인터페이스 사용 |
| 트랜잭션을 Controller에서 관리 | Service 레이어에서만 `@Transactional` |
| 여러 도메인 로직을 하나의 Service에 | 각 도메인의 Service를 Facade에서 조합 |
| S3 URL 직접 노출 | Presigned URL만 반환 |
| 비밀번호/민감정보를 Response DTO에 포함 | 필드 제외 또는 별도 DTO 사용 |
| 예외 메시지를 Controller에서 직접 작성 | ErrorCode enum 활용 |

---

## 8. 기술 스택 요약

| 구분 | 기술 |
|------|------|
| 언어/런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.3.0 |
| ORM | Spring Data JPA + Hibernate |
| 데이터베이스 | MySQL |
| 캐시 | Redis |
| 인증 | JWT (jjwt 0.11.5) + OAuth2 |
| 오브젝트 스토리지 | AWS S3 (SDK v2) |
| 푸시 알림 | Firebase Admin SDK |
| 매퍼 | MapStruct 1.6.3 |
| 코드 생성 | Lombok |
| API 문서 | SpringDoc OpenAPI (Swagger) |
| 빌드 | Gradle |

---

## 9. 참고 — 잘 구현된 모듈 예시

| 모듈 | 참고 이유 |
|------|----------|
| `ranking` | Facade 패턴, Redis 활용, Presigned URL 통합 |
| `meal` | Command/Result DTO 분리, 깔끔한 레이어 구조 |
| `mission` | 복잡한 도메인 로직, 배치 처리, 스케줄러 패턴 |
| `auth` | 외부 API 연동(OAuth), 인프라 레이어 분리 |
| `user` | 이미지 업로드 플로우, 캐싱 전략 |
