plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.datasource"
}

dependencies {

    /** Project */
    implementation(projects.core.utils)
    implementation(projects.libs.auth)
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.txhistory.models)

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
    implementation(deps.okHttp)
    implementation(deps.okHttp.prettyLogging)
    implementation(deps.retrofit)
    implementation(deps.retrofit.moshi)
    implementation(deps.reactive.network)

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
}
