plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion
}