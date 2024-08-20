plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.common"
}

dependencies {
    /* Core */
    implementation(projects.core.datasource)

    /* Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.tokens.models)

    /* Libs - SDK */
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)
    implementation(projects.libs.blockchainSdk)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Libs - Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.jodatime)
    implementation(deps.timber)
    implementation(deps.arrow.core)
}
