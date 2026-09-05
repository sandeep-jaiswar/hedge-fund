plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:oecd"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.oecd.ingest.Main") }
