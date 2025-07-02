plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.tokens"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    /** Project - Domain */
    api(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.staking)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.settings)
    implementation(projects.features.swap.domain.api)
    implementation(projects.features.swap.domain.models)
    implementation(projects.domain.promo.models)
    implementation(projects.domain.promo)
    implementation(projects.domain.networks)
    implementation(projects.domain.quotes)

    /** Project - Api */
    implementation(projects.features.staking.api)
    implementation(projects.features.markets.api)
    implementation(projects.features.swap.api)

    /** Project - Other */
    implementation(projects.core.configToggles)
    implementation(projects.core.utils)
    implementation(projects.libs.crypto)

    /** Android - Other */
    implementation(deps.androidx.paging.runtime)
    implementation(deps.timber)

    /** Utils */
    implementation(deps.jodatime)
    implementation(deps.reKotlin)

    /** Tests */
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    testImplementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
}