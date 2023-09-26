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

    /** Feature Apis */
    implementation(projects.features.wallet.api)

    /** Dependencies */
    implementation(deps.jodatime)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}