plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.referral"
}
dependencies {

    /** Libs */
    implementation(projects.core.utils)

    /** Core modules */
    implementation(projects.libs.crypto)

    /** Domain modules */
    implementation(projects.domain.card)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** Feature Apis */
    implementation(projects.features.tester.api)
    implementation(projects.features.wallet.api)

    /** Dependencies */
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.timber)
    implementation(deps.tangem.card.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}