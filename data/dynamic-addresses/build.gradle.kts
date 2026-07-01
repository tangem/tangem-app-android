plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.dynamicaddresses"
}
dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    // endregion

    // region Project - Libs
    implementation(tangemDeps.blockchain) { exclude(module = "joda-time") }
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.configToggles)
    api(projects.core.utils)
    implementation(projects.core.datasource)
    // endregion

    // region Project - Data
    api(projects.data.common)
    // endregion

    // region Project - Domain
    api(projects.domain.account)
    api(projects.domain.common)
    api(projects.domain.dynamicAddresses)
    api(projects.domain.models)
    api(projects.domain.walletManager)
    implementation(projects.domain.dynamicAddresses.models)
    // endregion

    // region Testing
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    // endregion
}