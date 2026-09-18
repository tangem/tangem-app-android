plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    testImplementation(deps.test.junit5)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.truth)
}