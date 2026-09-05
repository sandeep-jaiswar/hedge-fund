plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:calcfi"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.calcfi.ingest.Main") }
