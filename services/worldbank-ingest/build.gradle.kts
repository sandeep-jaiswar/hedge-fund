plugins {
    id("hedgefund.java-service")
}

dependencies {
    implementation(project(":libs:worldbank"))
    implementation(project(":libs:datalake"))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("com.hedgefund.worldbank.ingest.Main")
}
