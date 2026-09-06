plugins {
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    java
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

// repositories managed by settings.gradle.kts FAIL_ON_PROJECT_REPOS

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jobrunr)
    implementation(libs.jackson.databind)
    implementation(libs.snakeyaml)
    implementation(libs.slf4j.api)
    implementation(libs.spring.boot.starter.jdbc)
    runtimeOnly(libs.h2)
    runtimeOnly(libs.logback.classic)

    // depend on all ingest libs for delegation
    implementation(project(":libs:common"))
    implementation(project(":libs:datalake"))
    implementation(project(":libs:worldbank"))
    implementation(project(":libs:yahoo"))
    implementation(project(":libs:cboe"))
    implementation(project(":libs:binance"))
    implementation(project(":libs:coinbase"))
    implementation(project(":libs:defillama"))
    implementation(project(":libs:tencent"))
    implementation(project(":libs:sina"))
    implementation(project(":libs:eastmoney"))
    implementation(project(":libs:baostock"))
    implementation(project(":libs:investing"))
    implementation(project(":libs:fred"))
    implementation(project(":libs:treasury"))
    implementation(project(":libs:sec"))
    implementation(project(":libs:imf"))
    implementation(project(":libs:oecd"))
    implementation(project(":libs:calcfi"))
    implementation(project(":libs:fdic"))
    implementation(project(":libs:eia"))
    implementation(project(":libs:bls"))
    implementation(project(":libs:bea"))
    implementation(project(":libs:gmd"))

    testImplementation(libs.junit.jupiter)
}

tasks.withType<Test> { useJUnitPlatform() }

springBoot { mainClass.set("com.hedgefund.ingestionui.IngestionUiApplication") }
