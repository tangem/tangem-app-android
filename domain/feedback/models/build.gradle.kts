plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.ksp)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}
dependencies {
    /* Other */
    implementation(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.kotlin.serialization)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.visa.models)
}