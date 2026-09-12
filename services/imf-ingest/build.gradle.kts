plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:imf"))
    implementation(project(":libs:config-core"))
    implementation(project(":libs:observability"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.imf.ingest.Main") }
