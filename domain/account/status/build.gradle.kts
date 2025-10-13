plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.account.status"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    api(projects.domain.account)
    api(projects.domain.core)
    api(projects.domain.common)
    api(projects.domain.quotes)
    api(projects.domain.models)
    api(projects.domain.networks)
    api(projects.domain.staking)
    api(projects.domain.tokens)
    api(projects.domain.wallets)

    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)

    implementation(deps.kotlin.datetime)
    implementation(deps.kotlin.serialization)
    implementation(deps.timber)

    implementation(tangemDeps.blockchain)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // end

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(tangemDeps.blockchain)
    testImplementation(projects.common.test)
}