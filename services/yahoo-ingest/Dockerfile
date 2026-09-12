FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradle gradle && COPY gradlew . && COPY settings.gradle.kts .
COPY gradle/libs.versions.toml gradle/
COPY build-logic build-logic
COPY libs libs
COPY services/yahoo-ingest services/yahoo-ingest
RUN ./gradlew :services:yahoo-ingest:installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 app && adduser -u 1001 -G app -s /bin/sh -D app
WORKDIR /app
COPY --from=builder /workspace/services/yahoo-ingest/build/install/yahoo-ingest/ ./
COPY config config
COPY datalake/data datalake/data
COPY datalake/catalog datalake/catalog
RUN chown -R app:app /app
USER app
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8081/health || exit 1
ENTRYPOINT ["bin/yahoo-ingest"]
