plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:investing"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.investing.ingest.Main") }
