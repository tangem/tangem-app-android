import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.room)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.datasource"

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {

    /** Project */
    implementation(projects.core.analytics)
    implementation(projects.core.utils)
    implementation(projects.libs.auth)
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.core)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.models)
    implementation(projects.domain.nft.models)
    implementation(projects.domain.walletConnect.models)

    /** Tangem libraries */
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Coroutines */
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.coroutines.rx2)

    /** Logging */
    implementation(deps.timber)

    /** Network */
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.moshi.adapters)
    implementation(deps.moshi.adapters.ext)
    implementation(deps.okHttp)
    implementation(deps.okHttp.prettyLogging)
    implementation(deps.retrofit)
    implementation(deps.retrofit.moshi)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    /** Time */
    implementation(deps.jodatime)

    /** Security */
    implementation(deps.spongecastle.core)

    /** Chucker */
    debugImplementation(deps.chucker)
    mockedImplementation(deps.chuckerStub)
    externalImplementation(deps.chuckerStub)
    internalImplementation(deps.chuckerStub)
    releaseImplementation(deps.chuckerStub)

    /** Local storages */
    implementation(deps.androidx.datastore)
    implementation(deps.room.runtime)
    implementation(deps.room.ktx)
    ksp(deps.room.compiler)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}