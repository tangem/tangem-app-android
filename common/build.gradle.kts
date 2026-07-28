plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common"
}

dependencies {

    // region Other libraries
    implementation(deps.hilt.android)
    // endregion

    // region Firebase libraries
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    implementation(deps.firebase.messaging)
    // endregion

    // region Core modules
    implementation(projects.core.utils)
    // endregion

    // region Domain
    api(projects.domain.models)
    // endregion

    // region Libs
    // libs:crypto is intentionally re-exported (api): :common is ubiquitous and many feature modules
    // use BlockchainUtils / crypto helpers through it. Demoting to implementation cascades across the
    // feature graph, so keep it api despite DAGP's advice (suppressed below).
    api(projects.libs.crypto)
    // endregion

    // region Tests
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.truth)
    testImplementation(projects.test.core)
    // endregion
}