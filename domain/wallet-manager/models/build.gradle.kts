plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

dependencies {
    implementation(projects.domain.models)

    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.moshi.adapters)
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)
    implementation(projects.core.error)
}