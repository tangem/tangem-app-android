plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.tokens"
}

dependencies {

    /** Project - Domain */
    implementation(projects.domain.core)
    implementation(projects.domain.demo)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /** Project - Data */
    implementation(projects.core.datasource)
    implementation(projects.data.common)

    /** Project - Utils */
    implementation(projects.core.utils)
    implementation(projects.domain.legacy)

    /** Tangem SDKs */
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi.kotlin)
    implementation(deps.jodatime)
    implementation(deps.timber)
}
