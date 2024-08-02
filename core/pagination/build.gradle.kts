plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {
    // region Coroutines
    implementation(deps.kotlin.coroutines)
    // endregion
}