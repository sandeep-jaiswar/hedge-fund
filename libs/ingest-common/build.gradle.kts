plugins { id("hedgefund.java-library") }
dependencies {
    api(project(":libs:common"))
    api(libs.jackson.databind)
    api(libs.slf4j.api)
    api(libs.snakeyaml)
}
