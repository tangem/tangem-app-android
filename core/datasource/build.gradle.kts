import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.room)
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

    /** Tangem libraries */
    implementation(deps.tangem.blockchain)
    implementation(deps.tangem.card.core)

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
    implementation(deps.okHttp)
    implementation(deps.okHttp.prettyLogging)
    implementation(deps.retrofit)
    implementation(deps.retrofit.moshi)
    kaptForObfuscatingVariants(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    /** Time */
    implementation(deps.jodatime)

    /** Security */
    implementation(deps.spongecastle.core)

    /** Chucker */
    debugImplementation(deps.chucker)
    debugPGImplementation(deps.chucker)
    mockedImplementation(deps.chuckerStub)
    externalImplementation(deps.chuckerStub)
    internalImplementation(deps.chuckerStub)
    releaseImplementation(deps.chuckerStub)

    /** Local storages */
    implementation(deps.androidx.datastore)
    implementation(deps.room.runtime)
    implementation(deps.room.ktx)
    kapt(deps.room.compiler)

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
}