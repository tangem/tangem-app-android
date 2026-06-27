plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.txhistory"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(projects.data.common)

    implementation(projects.core.utils)
    implementation(projects.core.datasource)
    implementation(projects.core.pagination)
    implementation(projects.core.analytics)

    implementation(projects.domain.legacy)
    implementation(projects.domain.common)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.express.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.onramp)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)

    implementation(projects.libs.blockchainSdk)

    implementation(deps.kotlin.coroutines)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.jodatime)
    implementation(tangemDeps.blockchain)

    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    // region Test
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
    testRuntimeOnly(deps.test.junit5.engine)
    // endregion
}