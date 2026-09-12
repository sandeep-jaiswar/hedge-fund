plugins { id("hedgefund.java-library") }
dependencies {
    api(project(":libs:common"))
    api(project(":libs:datalake"))
    api(project(":libs:ingest-framework"))
    api(project(":libs:observability"))
    api(libs.jackson.databind)
    api(libs.slf4j.api)
    api(libs.snakeyaml)
    api(libs.duckdb.jdbc)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.wiremock)
}
