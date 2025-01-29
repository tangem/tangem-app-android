plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

dependencies {

    /** Project - Domain */
    implementation(projects.core.utils)
    implementation(projects.domain.core)
    implementation(projects.domain.wallets.models)
    implementation(deps.moshi.kotlin)
    kapt(deps.moshi.kotlin.codegen)
}