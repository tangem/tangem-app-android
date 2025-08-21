plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.staking"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    api(projects.domain.staking.models)
    api(projects.domain.core)
    api(projects.core.analytics)
    api(projects.core.utils)

    implementation(deps.kotlin.datetime)
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)

    implementation(projects.domain.legacy)
    implementation(projects.domain.walletManager) // TODO refactor to use from data module
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    implementation(projects.features.staking.api)

    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(projects.libs.crypto)
    implementation(projects.libs.blockchainSdk)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(tangemDeps.card.core)
    testImplementation(projects.common.test)
}