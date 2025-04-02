plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.networks"
}

dependencies {
    implementation(projects.libs.blockchainSdk)

    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(projects.data.common)
    implementation(projects.data.tokens)

    implementation(projects.domain.core)
    implementation(projects.domain.demo)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.networks)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.androidx.datastore)
    implementation(deps.moshi)
    implementation(deps.timber)

    implementation(tangemDeps.blockchain)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(tangemDeps.card.core)
    testImplementation(projects.common.test)
}