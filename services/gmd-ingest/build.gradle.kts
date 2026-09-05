plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:gmd"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.gmd.ingest.Main") }
