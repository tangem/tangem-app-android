plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.account"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    // region Project - Common
    implementation(projects.common.ui) // It's needed for getting AccountName.DefaultMain value
    // endregion

    // region Project - Core
    implementation(projects.core.datasource)
    implementation(projects.core.configToggles)
    implementation(projects.core.res)
    api(projects.core.utils)
    // endregion

    // region Project - Domain
    api(projects.domain.account)
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.models)
    api(projects.domain.tokens)
    api(projects.domain.wallets)
    // endregion

    // region Project - Data
    implementation(projects.data.common)
    // endregion

    // region Project - Libs
    implementation(projects.libs.crypto)
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tangem dependencies
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region AndroidX libraries
    implementation(deps.androidx.datastore)
    // endregion

    // region Other Dependencies
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    // endregion

    // region Test
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    // endregion
}