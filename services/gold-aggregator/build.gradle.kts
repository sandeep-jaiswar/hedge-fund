plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:datalake"))
    implementation(project(":libs:common"))
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
    implementation(libs.duckdb.jdbc)
}
application { mainClass.set("com.hedgefund.gold.Main") }
