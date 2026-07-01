plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    api(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Core modules
    api(projects.core.pagination)
    // endregion

    // region Domain
    api(projects.domain.models)
    // endregion
}