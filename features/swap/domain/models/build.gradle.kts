import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.swap.domain.models"
}

dependencies {
    /** Domain */
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)

    /** Core modules */
    implementation(projects.core.utils)
    implementation(projects.core.datasource)

    /** Other Libraries **/
    implementation(deps.kotlin.serialization)
    implementation(deps.arrow.core)
    implementation(deps.moshi.kotlin)
    kaptForObfuscatingVariants(deps.moshi.kotlin.codegen)

}