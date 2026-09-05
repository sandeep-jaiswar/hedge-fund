plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

// Note: test deps (junit, mockito) are added via version catalog in each module's build.gradle.kts
// to keep DRY without requiring VersionCatalogsExtension lookup inside convention plugin.
