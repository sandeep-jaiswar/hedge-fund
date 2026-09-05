# hedge-fund Monorepo

Java 21 monorepo - DRY + SIMPLE principles.

## Structure

```
.
├── gradle/                 # wrapper + version catalog (single source of truth)
│   ├── libs.versions.toml  # all dependency versions - DRY
│   └── wrapper/
├── build-logic/            # convention plugins - DRY shared build logic
│   └── src/main/kotlin/
│       ├── hedgefund.java-common.gradle.kts   # Java 21, JUnit 5, UTF-8
│       ├── hedgefund.java-library.gradle.kts  # for libs/*
│       └── hedgefund.java-service.gradle.kts  # for apps/*, services/*
├── libs/
│   └── common/             # shared code (Money, utils) - DRY
├── apps/
│   └── api/                # deployable app (executable jar)
└── services/
    └── worker/             # background service (executable jar)
```

## Principles

- **DRY**: version catalog + convention plugins. No duplicated `java { toolchain }` or test deps in each module.
- **SIMPLE**: No Spring Boot bloat by default. Plain Java 21 + SLF4J + Jackson. Add frameworks only when needed per module.
- **Java 21**: toolchain + `release=21`, virtual threads ready.

## Requirements

- Java 21 (Corretto 21 tested)
- No local Gradle install needed - uses wrapper (`./gradlew`)

## Quick Start

```bash
./gradlew build          # build all
./gradlew test           # run all tests
./gradlew :apps:api:run  # run API
./gradlew :services:worker:run

./gradlew projects       # list modules
./gradlew printProjects  # custom helper
```

## Adding a New Module

**Library:**
```bash
mkdir -p libs/my-lib/src/main/java/com/hedgefund/mylib
# create libs/my-lib/build.gradle.kts:
#   plugins { id("hedgefund.java-library") }
#   dependencies { implementation(project(":libs:common")) }
# add to settings.gradle.kts: include(":libs:my-lib")
```

**App/Service:**
```bash
mkdir -p apps/my-app/src/main/java/com/hedgefund/myapp
# build.gradle.kts:
#   plugins { id("hedgefund.java-service") }
#   application { mainClass.set("com.hedgefund.myapp.Main") }
# add to settings.gradle.kts: include(":apps:my-app")
```

Add shared deps to `gradle/libs.versions.toml` only - don't hardcode versions in modules.

## Build Logic

All shared Gradle config lives in `build-logic/`. Change Java version or test config there once, not in N modules.

## Commitlint (native)

Conventional Commits enforced via **commitlint + husky** (native npm, no Docker).

- Config: `commitlint.config.js:1` (`extends: @commitlint/config-conventional`)
- Hook: `.husky/commit-msg:1` → `npx --no -- commitlint --edit $1`
- Gradle tasks: `./gradlew commitLint` / `./gradlew commitLintRange`

```bash
npm install                  # install @commitlint/cli, husky
npm run commitlint           # lint origin/main..HEAD
./gradlew commitLint         # same via Gradle (verification group)

# test a message
echo "feat(api): add pricing" | npx commitlint        # passes
echo "bad message" | npx commitlint                    # fails: type-empty
```

Allowed types: `feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert` (conventional). Header max 100, body line 150 (`commitlint.config.js:8`).

Bypass only if needed: `git commit --no-verify` (not recommended).
