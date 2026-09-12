plugins { id("hedgefund.java-service") }
dependencies {
    implementation(project(":libs:eastmoney"))
    implementation(project(":libs:config-core"))
    implementation(project(":libs:observability"))
    implementation(libs.slf4j.api)
    implementation(libs.logback.classic)
}
application { mainClass.set("com.hedgefund.eastmoney.ingest.Main") }
