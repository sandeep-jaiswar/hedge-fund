// Root build file - keeps it SIMPLE
// All shared Java config lives in build-logic convention plugins (DRY)
// Wiring: ingestion-ui is the monorepo control plane (JobRunr) -> syncs to Floci S3

tasks.register<Exec>("runIngestionUi") {
    group = "ingestion"
    description = "Run ingestion-ui control plane (JobRunr :8080 + :8000/dashboard)"
    commandLine("./gradlew", ":apps:ingestion-ui:bootRun")
}

tasks.register<Exec>("syncFloci") {
    group = "datalake"
    description = "Sync datalake/data/{bronze,silver,gold} -> s3://hedge-* via Floci http://localhost:4566"
    commandLine("bash", "-c", "python3 datalake/scripts/sync-all-to-floci.py")
}

tasks.register<Exec>("provisionFloci") {
    group = "datalake"
    description = "Provision Floci buckets + Glue DBs + Firehose"
    commandLine("bash", "-c", "python3 datalake/scripts/provision-floci.py")
}

tasks.register<Exec>("startFloci") {
    group = "datalake"
    description = "Start Floci 2.0.1 (requires sudo; set FLOCI_SUDO_PASSWORD env or gradle property flociSudoPassword)"
    val sudoPass = providers.environmentVariable("FLOCI_SUDO_PASSWORD")
        .orElse(providers.gradleProperty("flociSudoPassword")).getOrElse("")
    if (sudoPass.isNotEmpty()) {
        commandLine("bash", "-c", "echo \"\$FLOCI_SUDO_PASSWORD\" | sudo -S floci start && floci doctor")
        environment("FLOCI_SUDO_PASSWORD", sudoPass)
    } else {
        commandLine("bash", "-c", "sudo floci start && floci doctor")
    }
}

tasks.register("monorepoStatus") {
    group = "help"
    description = "Prints monorepo wiring (apps, services, datalake, Floci)"
    doLast {
        println("Monorepo: ${gradle.rootProject.allprojects.size} projects")
        println(" - Control plane: apps/ingestion-ui :8080 (UI) + :8000/dashboard (JobRunr)")
        println(" - Datalake: datalake/data/{bronze 24, silver 23, gold 4} -> s3://hedge-* syncFloci")
        println(" - Floci: http://localhost:4566 _floci/health v2.0.1")
        gradle.rootProject.allprojects.forEach { println("   - ${it.path}") }
    }
}

tasks.register<Exec>("commitLint") {
    group = "verification"
    description = "Lint last commit (HEAD) with commitlint — native npm, conventional-commits"
    // handles single-commit repo (HEAD~1 missing) gracefully
    commandLine("bash", "-c", "git rev-parse HEAD~1 >/dev/null 2>&1 && npx --no commitlint --from=HEAD~1 --to=HEAD --verbose || git log -1 --pretty=%B | npx --no commitlint --verbose")
}

tasks.register<Exec>("commitLintRange") {
    group = "verification"
    description = "Lint commits from origin/main to HEAD"
    commandLine("bash", "-c", "git rev-parse origin/main >/dev/null 2>&1 && npx --no commitlint --from=origin/main --to=HEAD --verbose || npx --no commitlint --from=HEAD~1 --to=HEAD --verbose || git log -1 --pretty=%B | npx --no commitlint --verbose")
}

tasks.register("printProjects") {
    group = "help"
    description = "Prints all projects in the monorepo"
    doLast {
        println("Monorepo projects:")
        gradle.rootProject.allprojects.forEach { println(" - ${it.path}") }
    }
}
