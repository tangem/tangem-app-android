plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Coroutines
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Time dependencies
    implementation(deps.jodatime)
    // endregion
}
