plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {
    /** Libs */
    implementation(project(":libs:crypto"))
    implementation(project(":core:utils"))

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Other Libraries **/
    implementation(deps.kotlin.serialization)
}
