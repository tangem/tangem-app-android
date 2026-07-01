plugins {
    alias(deps.plugins.kotlin.jvm)
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
    api(projects.domain.settings)
    // endregion

    // region Domain models
    api(projects.domain.stories.models)
    // endregion
}