plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:treasury"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.treasury.ingest.Main") }
