plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.payment"
}

dependencies {
    /** Project - Data */
    implementation(projects.core.error)
    implementation(projects.core.error.ext)
    implementation(projects.data.common)
    implementation(projects.data.wallets)

    /** Project - Domain */
    implementation(projects.domain.payment)
    implementation(projects.domain.payment.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.models)
    implementation(projects.domain.common)

    /** Project - Utils */
    implementation(projects.core.utils)
    implementation(projects.domain.legacy)
    implementation(projects.libs.blockchainSdk)

    /** Libs - Tangem */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.card.android)
    implementation(tangemDeps.hot.core)
    implementation(projects.libs.tangemSdkApi)

    /** Libs - Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.moshi.kotlin)
    ksp(deps.moshi.kotlin.codegen)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}
