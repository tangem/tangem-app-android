plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.legacy"
}

dependencies {
    implementation(projects.common)
    implementation(projects.domain.models)
    implementation(projects.domain.card)
    implementation(projects.domain.legacy)
    implementation(projects.domain.wallets.models)

    implementation(projects.core.res)

    /** Tangem libraries */
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.card.android) {
        exclude(module = "joda-time")
    }

    /** Other libraries */
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}
