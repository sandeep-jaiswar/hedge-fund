plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:yahoo"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.yahoo.ingest.Main") }
