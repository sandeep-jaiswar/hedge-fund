plugins { id("hedgefund.java-library") }
dependencies {
    api(project(":libs:datalake"))
    api(project(":libs:ingest-framework"))
    api(libs.junit.jupiter)
    api(libs.wiremock)
    api(libs.jackson.databind)
}
