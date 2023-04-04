plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Coroutines */
    implementation(deps.kotlin.coroutines)
}