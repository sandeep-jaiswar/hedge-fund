plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:sec"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.sec.ingest.Main") }
