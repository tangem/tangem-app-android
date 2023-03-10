plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {

    /** Libs */
    implementation(project(":core:utils"))
    implementation(project(":libs:crypto"))

    /** Time */
    implementation(deps.jodatime)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}
