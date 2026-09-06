plugins {
    id("hedgefund.java-library")
    application
}

application {
    mainClass.set("com.hedgefund.datalake.Main")
}

dependencies {
    api(project(":libs:common"))
    api(libs.jackson.databind)
    api(libs.slf4j.api)
    // DuckDB = local Athena (no Docker)
    api(libs.duckdb.jdbc)
    api(libs.liquibase.core)
    // Optional AWS SDK + Floci Testcontainers for when Docker is re-enabled
    compileOnly(libs.aws.s3)
    compileOnly(libs.aws.glue)
    compileOnly(libs.aws.athena)
    compileOnly(libs.aws.firehose)
    compileOnly(libs.testcontainers.floci)

    runtimeOnly(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    // testContainers only if Docker is available; tests skip gracefully when not
    testImplementation(libs.testcontainers.floci)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.aws.s3)
    testImplementation(libs.aws.glue)
}

tasks.test {
    // Pass through to allow skipping floci tests when Docker unavailable
    systemProperty("floci.test.enabled", findProperty("floci.test.enabled") ?: "false")
}
