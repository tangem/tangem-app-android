plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.jodatime)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion
}