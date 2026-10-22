plugins {
    alias(deps.plugins.kotlin.jvm)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Core modules
    implementation(projects.core.utils)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion
}