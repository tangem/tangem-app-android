plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

dependencies {
    /** Project - Core */
    implementation(projects.core.error)

    /** Domain models */
    implementation(projects.domain.models)

    /** Libs - Tangem */
    implementation(tangemDeps.card.core)

    /** Libs - Other */
    implementation(deps.moshi.adapters)
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
}
