plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.swap"
}

dependencies {
    /** Libs */
    implementation(projects.libs.crypto)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Domain */
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** Core modules */
    implementation(projects.core.utils)

    /** Feature Apis */
    implementation(projects.features.wallet.api)

    /** Other Libraries **/
    implementation(deps.kotlin.serialization)
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.timber)
}
