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

    // region Core modules
    api(projects.core.error)
    // endregion

    // region Tangem SDK (derived public keys types for VA activation)
    api(tangemDeps.card.core)
    // endregion

    // region Domain models
    implementation(projects.domain.models)
    // endregion
}