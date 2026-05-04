plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

dependencies {
    // region Tangem libraries
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.hot.core)
    // endregion

    // region Domain modules
    implementation(projects.domain.models)
    // endregion

    implementation(projects.core.utils)

    // region Other libraries
    implementation(deps.kotlin.serialization)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
    // endregion
}