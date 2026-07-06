plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Domain
    api(projects.domain.core)
    api(projects.domain.models)
    // endregion

    // region Domain models
    api(projects.domain.express.models)
    // endregion
}