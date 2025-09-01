plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.lib.crypto"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    // region Project
    implementation(projects.core.utils)
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tangem SDKs
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
    // endregion

    // region Other deps
    implementation(deps.kotlin.coroutines)
    implementation(deps.timber)
    // endregion

    // region Test libraries
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.truth)
    // endregion
}