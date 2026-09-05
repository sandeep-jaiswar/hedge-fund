plugins {
    id("hedgefund.java-common")
    application
}

// Service = runnable jar with main class
// Override `application { mainClass.set(...) }` in each service/app

tasks.named<JavaExec>("run") {
    // pass through
}

application {
    // toolchain already set via java-common
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
    }
    // For SIMPLE fat-jar without Spring Boot, uncomment if needed:
    // duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
