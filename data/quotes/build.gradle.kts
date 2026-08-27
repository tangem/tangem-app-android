plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.quotes"
}
dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.moshi)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    // endregion

    // region Tangem SDKs
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.datasource)
    implementation(projects.core.local)
    api(projects.core.utils)
    // endregion

    // region Project - Data
    api(projects.data.common)
    // endregion

    // region Project - Domain
    api(projects.domain.core)
    api(projects.domain.quotes)
    implementation(projects.domain.models)
    // endregion

    // region Project - Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    // endregion
}