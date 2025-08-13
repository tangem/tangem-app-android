plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.networks"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // region Project - Core
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Project - Data
    implementation(projects.data.common)
    // endregion

    // region Project - Domain
    implementation(projects.domain.legacy)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.card)
    api(projects.domain.models)
    implementation(projects.domain.networks)
    // endregion

    // region Project - Libs
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Other libraries
    implementation(deps.androidx.datastore)
    implementation(deps.moshi)
    implementation(deps.timber)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(tangemDeps.blockchain)
    testImplementation(tangemDeps.card.core)
    testImplementation(projects.common.test)
    // endregion
}