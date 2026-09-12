plugins {
    id("hedgefund.java-service")
}

dependencies {
    implementation(project(":libs:bls"))
    implementation(project(":libs:datalake"))
    implementation(project(":libs:config-core"))
    implementation(project(":libs:observability"))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("com.hedgefund.bls.ingest.Main")
}
