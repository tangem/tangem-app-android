plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

dependencies {

    // region Kotlin
    api(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.jodatime)
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    // endregion

    // region Domain models
    api(projects.domain.models)
    // endregion
}