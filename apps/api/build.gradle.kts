plugins {
    id("hedgefund.java-service")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
}

application {
    mainClass.set("com.hedgefund.api.Main")
}
