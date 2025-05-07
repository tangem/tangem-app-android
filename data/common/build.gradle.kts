plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.common"
}

dependencies {
    /* Core */
    implementation(projects.core.datasource)

    /* Domain */
    implementation(projects.domain.demo)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /* Libs - SDK */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    implementation(projects.libs.blockchainSdk)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /* Libs - Other */
    implementation(deps.androidx.datastore)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.timber)

    /* Test */
    testImplementation(projects.common.test)
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}