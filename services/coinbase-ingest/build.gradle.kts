plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:coinbase"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.coinbase.ingest.Main") }
