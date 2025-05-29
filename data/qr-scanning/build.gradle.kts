plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.qrscanning"
}

dependencies {

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.qrScanning)
    implementation(projects.domain.qrScanning.models)
    implementation(projects.domain.tokens.models)

    implementation(projects.core.ui)
    implementation(projects.libs.blockchainSdk)

    /** SdK */
    implementation(tangemDeps.blockchain)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}