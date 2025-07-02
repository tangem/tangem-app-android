plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.swap.domain.models"
}

dependencies {
    /** Domain */
    api(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)

    /** Core modules */
    implementation(projects.core.utils)
    implementation(projects.core.datasource)

    /** Other Libraries **/
    implementation(tangemDeps.blockchain)
    implementation(deps.kotlin.serialization)
    implementation(deps.arrow.core)
    implementation(deps.moshi.kotlin)
    implementation(deps.jodatime)
    ksp(deps.moshi.kotlin.codegen)

}