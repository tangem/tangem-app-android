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

    // region Kotlin
    api(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    api(deps.jodatime)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.datastore)
    // endregion

    // region Libs - SDK
    api(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    implementation(projects.core.local)
    implementation(projects.store.datasource)
    api(projects.grow.datasource)
    api(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.common)
    implementation(projects.domain.appsflyer)
    api(projects.domain.walletManager)
    api(projects.domain.wallets)
    implementation(projects.domain.card)
    implementation(projects.domain.core)
    implementation(projects.domain.jointAccount)
    runtimeOnly(projects.domain.account)
    // endregion

    // region Domain models
    api(projects.domain.demo.models)
    api(projects.domain.express.models)
    api(projects.domain.models)
    implementation(projects.domain.wallets.models)
    // endregion

    // region Libs
    api(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    implementation(projects.features.jointAccount.api)

    // region Test
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}