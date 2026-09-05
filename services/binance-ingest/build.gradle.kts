plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:binance"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.binance.ingest.Main") }
