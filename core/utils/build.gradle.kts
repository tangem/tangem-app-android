plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}
dependencies {

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization)
    // endregion

    // region Time dependencies
    api(deps.jodatime)
    // endregion

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.mockk)
}