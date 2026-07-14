plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.account"
}
dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    // endregion

    // region Tangem dependencies
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.hot.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.core.res)
    // endregion

    // region Project - Domain
    api(projects.domain.account)
    api(projects.domain.common)
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.tokens)
    implementation(projects.domain.card)
    runtimeOnly(projects.domain.visa)
    runtimeOnly(projects.domain.wallets)
    // endregion

    // region Project - Data
    api(projects.data.common)
    // endregion

    // region Project - Features
    api(projects.features.virtualAccounts.details.api) // VIRTUAL_ACCOUNTS_ENABLED
    // endregion

    // region Project - Common
    implementation(projects.common.ui) // It's needed for getting AccountName.DefaultMain value
    // endregion

    // region Project - Libs
    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)
    // endregion

    // region Test
    testImplementation(deps.test.turbine)
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}