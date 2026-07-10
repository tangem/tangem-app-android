plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.serialization.core)
    // endregion

    // region Core modules
    api(projects.core.analytics.models)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion
}