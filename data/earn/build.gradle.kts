plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.earn"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.datasource)
    api(projects.core.utils)
    implementation(projects.core.pagination)
    // endregion

    // region Project - Data
    implementation(projects.data.common)
    // endregion

    // region Project - Domain
    api(projects.domain.common)
    api(projects.domain.earn)
    implementation(projects.domain.models)
    runtimeOnly(projects.domain.account.status)
    // endregion

    // region Project - Libs
    api(projects.libs.blockchainSdk)
    // endregion
}