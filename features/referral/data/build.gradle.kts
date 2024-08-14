plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.referral.data"
}

dependencies {

    /** Project */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Data modules */
    implementation(projects.data.common)

    /** Domain modules */
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.features.referral.domain)

    /** Libs */
    implementation(projects.libs.auth)

    /** Time */
    implementation(deps.jodatime)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tangem deps */
    implementation(deps.tangem.blockchain)
}