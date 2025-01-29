plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    implementation(deps.moshi.kotlin)
    kapt(deps.moshi.kotlin.codegen)
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)
}