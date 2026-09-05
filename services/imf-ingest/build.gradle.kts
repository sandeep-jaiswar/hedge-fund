plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:imf"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.imf.ingest.Main") }
