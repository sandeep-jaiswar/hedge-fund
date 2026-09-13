FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradle gradle && COPY gradlew . && COPY settings.gradle.kts .
COPY gradle/libs.versions.toml gradle/
COPY build-logic build-logic
COPY libs libs
COPY services/ingest-runner services/ingest-runner
COPY services/gold-aggregator services/gold-aggregator
RUN ./gradlew :services:ingest-runner:installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 app && adduser -u 1001 -G app -s /bin/sh -D app
WORKDIR /app
COPY --from=builder /workspace/services/ingest-runner/build/install/ingest-runner/ ./
COPY config config
COPY datalake/data datalake/data
COPY datalake/catalog datalake/catalog
RUN chown -R app:app /app
USER app
# Source selected via HEDGE_SOURCE env (compose) or --source arg
ENTRYPOINT ["sh", "-c", "bin/ingest-runner --source ${HEDGE_SOURCE:-yahoo}"]
