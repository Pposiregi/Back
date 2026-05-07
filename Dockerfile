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

# ── Stage 3: Runtime (JRE-alpine, ~165MB) ─────────────────────────────────────
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
RUN addgroup -S fitpet && adduser -S fitpet -G fitpet

WORKDIR /app

# 의존성(변경 빈도 낮은 순) → 레이어 캐시 최적화
COPY --from=extractor /workspace/dependencies/          ./
COPY --from=extractor /workspace/spring-boot-loader/    ./
COPY --from=extractor /workspace/snapshot-dependencies/ ./
COPY --from=extractor /workspace/application/           ./

RUN mkdir -p /app/logs && chown -R fitpet:fitpet /app
USER fitpet

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
