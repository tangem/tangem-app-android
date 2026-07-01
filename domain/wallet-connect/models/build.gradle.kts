plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.ksp)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}
dependencies {

    // region Kotlin
    api(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    // endregion

    // region Domain models
    api(projects.domain.blockaid.models)
    api(projects.domain.models)
    api(projects.domain.tokens.models)
    api(projects.domain.transaction.models)
    // endregion
}