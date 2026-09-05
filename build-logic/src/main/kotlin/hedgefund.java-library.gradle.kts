plugins {
    id("hedgefund.java-common")
    `java-library`
}

// Library-specific conventions (SIMPLE - no Spring, no extra magic)
tasks.jar {
    manifest {
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] = project.version
    }
}
