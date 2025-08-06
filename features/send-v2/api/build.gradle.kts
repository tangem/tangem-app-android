plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.send.v2.api"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.analytics.models)

    /** Common */
    implementation(projects.common.ui)

    /** Domain models */
    api(projects.domain.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets.models)

    /** Compose */
    implementation(deps.compose.runtime)
    implementation(deps.compose.foundation)

    /** Tangem */
    implementation(tangemDeps.blockchain)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    testImplementation(projects.domain.staking.models)
    // endregion
}