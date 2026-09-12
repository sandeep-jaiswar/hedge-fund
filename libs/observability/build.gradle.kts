plugins { id("hedgefund.java-library") }
dependencies {
    api(libs.slf4j.api)
    api(libs.logback.classic)
    api(libs.jackson.databind)
    testImplementation(libs.junit.jupiter)
}
