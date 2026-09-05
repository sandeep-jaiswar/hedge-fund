plugins {
    id("hedgefund.java-library")
}

dependencies {
    api(project(":libs:common"))
    api(project(":libs:datalake"))
    api(libs.jackson.databind)
    api(libs.slf4j.api)
    api(libs.snakeyaml)
    api(libs.duckdb.jdbc)

    runtimeOnly(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.wiremock)
}
