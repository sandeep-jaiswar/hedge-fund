plugins { id("hedgefund.java-library") }
dependencies {
    api(libs.slf4j.api)
    api(libs.snakeyaml)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
}
