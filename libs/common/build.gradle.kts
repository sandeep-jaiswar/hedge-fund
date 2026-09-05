plugins {
    id("hedgefund.java-library")
}

dependencies {
    api(libs.slf4j.api)
    api(libs.jackson.databind)
    runtimeOnly(libs.logback.classic)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
}
