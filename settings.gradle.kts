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
include(":libs:ingest-common")
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
include(":services:worker")
include(":services:worldbank-ingest")
include(":services:yahoo-ingest")
include(":services:investing-ingest")
include(":services:baostock-ingest")
include(":services:eastmoney-ingest")
include(":services:sina-ingest")
include(":services:tencent-ingest")
include(":services:gmd-ingest")
include(":services:gold-aggregator")
include(":services:bea-ingest")
include(":services:bls-ingest")
include(":services:eia-ingest")
include(":services:fdic-ingest")
include(":services:calcfi-ingest")
include(":services:oecd-ingest")
include(":services:imf-ingest")
include(":services:sec-ingest")
include(":services:treasury-ingest")
include(":services:fred-ingest")
include(":services:defillama-ingest")
include(":services:coinbase-ingest")
include(":services:binance-ingest")
include(":services:cboe-ingest")
