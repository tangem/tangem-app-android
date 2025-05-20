plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.ksp)
    id("configuration")
}

dependencies {
    api(projects.domain.models)
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)
}