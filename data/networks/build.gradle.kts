plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.networks"
}
dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.moshi)
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    // endregion

    // region Project - Libs (SDK)
    implementation(tangemDeps.blockchain) { exclude(module = "joda-time") }
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.utils)
    implementation(projects.core.local)
    implementation(projects.core.datasource)
    // endregion

    // region Project - Data
    api(projects.data.common)
    api(projects.data.dynamicAddresses)
    // endregion

    // region Project - Domain
    api(projects.domain.common)
    api(projects.domain.core)
    api(projects.domain.models)
    api(projects.domain.networks)
    api(projects.domain.walletManager)
    runtimeOnly(projects.domain.card)
    // endregion

    // region Project - Libs
    api(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.domain.card)
    testImplementation(projects.test.core)
    // endregion
}