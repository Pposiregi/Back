# GCP VM 타겟 = amd64. Mac Apple Silicon에서도 올바른 플랫폼으로 빌드한다.
# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x gradlew && \
    ./gradlew bootJar -x test --no-daemon --quiet

# ── Stage 2: Layer extraction ─────────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-alpine AS extractor
WORKDIR /workspace

COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Stage 3: Runtime (JRE-alpine) ─────────────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
RUN addgroup -S fitpet && adduser -S fitpet -G fitpet

WORKDIR /app

# 의존성(변경 빈도 낮은 순) → 레이어 캐시 최적화
# COPY --chown으로 소유권을 즉시 지정 → 이후 chown -R 레이어 복제(약 147MB) 회피.
COPY --chown=fitpet:fitpet --from=extractor /workspace/dependencies/          ./
COPY --chown=fitpet:fitpet --from=extractor /workspace/spring-boot-loader/    ./
COPY --chown=fitpet:fitpet --from=extractor /workspace/snapshot-dependencies/ ./
COPY --chown=fitpet:fitpet --from=extractor /workspace/application/           ./

# logs 디렉토리만 생성 (소유권은 USER 전환 후 fitpet이 자동 보유).
RUN mkdir -p /app/logs && chown fitpet:fitpet /app/logs
USER fitpet

EXPOSE 8080

# JDK17 기본 GC가 G1이므로 -XX:+UseG1GC 제거 (중복).
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# exec로 sh를 java로 교체 → java가 PID 1이 되어 SIGTERM을 직접 수신, graceful shutdown 보장.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
