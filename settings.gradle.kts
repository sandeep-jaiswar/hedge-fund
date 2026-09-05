pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
    // libs.versions.toml is auto-discovered as catalog 'libs' - no manual `from` needed (avoids duplicate)
}

rootProject.name = "hedge-fund"

// libs - shared code (DRY)
include(":libs:common")

// apps - deployable applications
include(":apps:api")

// services - background services / microservices
include(":services:worker")
