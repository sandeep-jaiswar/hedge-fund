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
include(":libs:datalake")
include(":libs:ingest-framework")
include(":libs:config-core")
include(":libs:observability")
include(":libs:test-support")
include(":libs:worldbank")
include(":libs:yahoo")
include(":libs:investing")
include(":libs:baostock")
include(":libs:eastmoney")
include(":libs:sina")
include(":libs:tencent")
include(":libs:gmd")
include(":libs:bea")
include(":libs:bls")
include(":libs:eia")
include(":libs:fdic")
include(":libs:calcfi")
include(":libs:oecd")
include(":libs:imf")
include(":libs:sec")
include(":libs:treasury")
include(":libs:fred")
include(":libs:defillama")
include(":libs:coinbase")
include(":libs:binance")
include(":libs:cboe")

// apps - deployable applications
include(":apps:api")
include(":apps:ingestion-ui")

// services - background services / microservices
include(":services:ingest-runner")
include(":services:gold-aggregator")
