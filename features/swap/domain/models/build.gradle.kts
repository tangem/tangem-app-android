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

    /** Core modules */
    implementation(projects.core.utils)

    /** Other Libraries **/
    implementation(deps.kotlin.serialization)
    implementation(deps.arrow.core)

}