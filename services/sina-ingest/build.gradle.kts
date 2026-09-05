plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:sina"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.sina.ingest.Main") }
