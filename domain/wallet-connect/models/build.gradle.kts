plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.ksp)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}
dependencies {

    /* Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.blockaid.models)

    /* Other */
    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)

    /* Utils */
    implementation(deps.kotlin.serialization)
}