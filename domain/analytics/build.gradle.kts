plugins {
    alias(deps.plugins.kotlin.jvm)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

dependencies {

    /** Project - Domain */
    implementation(projects.core.utils)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)
    implementation(deps.moshi.kotlin)
    implementation(deps.arrow.core)
    implementation(deps.arrow.fx)
    ksp(deps.moshi.kotlin.codegen)
}