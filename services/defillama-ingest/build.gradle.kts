plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:defillama"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.defillama.ingest.Main") }
