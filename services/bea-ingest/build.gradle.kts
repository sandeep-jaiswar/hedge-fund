plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:bea"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.bea.ingest.Main") }
