plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.jointaccount"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    // endregion

    // region Other libraries
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    // endregion

    // region Tangem SDK
    implementation(tangemDeps.card.core)
    // AndroidSecureStorageV2 for the joint account invites (see com.tangem.data.jointaccount.store).
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    implementation(projects.core.local)
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Data
    implementation(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.jointAccount)
    implementation(projects.domain.common)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    // endregion

    // region Tangem SDKs
    implementation(tangemDeps.blockchain)
    // endregion

    // region Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tests
    testImplementation(projects.test.core)
    // endregion
}