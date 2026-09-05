plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:fred"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.fred.ingest.Main") }
