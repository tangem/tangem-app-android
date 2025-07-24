import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.tokens"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    // region Project - Data
    implementation(projects.data.common)
    implementation(projects.data.networks)
    // endregion

    // region Project - Domain
    implementation(projects.domain.core)
    implementation(projects.domain.demo)
    implementation(projects.domain.legacy)
    implementation(projects.domain.walletManager)
    implementation(projects.domain.card)
    implementation(projects.domain.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets.models)
    // endregion

    // region Project - Utils
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.libs.blockchainSdk)
    // endregion

    // region Tangem SDKs
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    // endregion

    // region AndroidX
    implementation(deps.androidx.datastore)
    implementation(deps.androidx.paging.runtime)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Other
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi.kotlin)
    implementation(deps.retrofit) // For HttpException
    implementation(deps.timber)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Tests
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(projects.common.test)
    // endregion
}