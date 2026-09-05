// Root build file - keeps it SIMPLE
// All shared Java config lives in build-logic convention plugins (DRY)

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
