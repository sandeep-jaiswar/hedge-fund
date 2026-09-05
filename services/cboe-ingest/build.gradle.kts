plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:cboe"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.cboe.ingest.Main") }
