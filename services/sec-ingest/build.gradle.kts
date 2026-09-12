plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:sec"))
    implementation(project(":libs:config-core"))
    implementation(project(":libs:observability"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.sec.ingest.Main") }
