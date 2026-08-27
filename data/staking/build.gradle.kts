import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.staking"
}

dependencies {

    // region Kotlin
    api(deps.kotlin.coroutines)
    implementation(deps.kotlin.datetime)
    // endregion

    // region Other libraries
    api(deps.androidx.datastore)
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.arrow.core)
    implementation(deps.firebase.crashlytics)
    implementation(deps.jodatime)
    implementation(deps.kotlin.immutable.collections)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.analytics)
    implementation(projects.core.local)
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    // endregion

    // region Data
    implementation(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.staking)
    api(projects.domain.walletManager)
    api(projects.domain.wallets)
    implementation(projects.domain.card)
    // endregion

    // region Domain models
    api(projects.domain.staking.models)
    implementation(projects.domain.blockaid.models)
    implementation(projects.domain.wallets.models)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}