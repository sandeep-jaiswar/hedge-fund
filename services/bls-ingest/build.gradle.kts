plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:bls"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.bls.ingest.Main") }
