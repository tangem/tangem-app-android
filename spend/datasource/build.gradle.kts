import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.spend.datasource"
}

dependencies {
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    implementation(deps.jodatime)
    implementation(deps.okio)

    api(deps.moshi)
    implementation(deps.moshi.adapters.ext)
    ksp(deps.moshi.kotlin.codegen)
    api(deps.retrofit)
    api(deps.jodatime)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    implementation(deps.hilt.android)
    implementation(deps.androidx.datastore)
    kapt(deps.hilt.kapt)

    api(projects.core.remote)
    api(projects.core.utils)
    // Exposed in public store interface signatures (UserWalletId, TangemPayReissueCardFee,
    // TangemPayTxHistoryItem, ...)
    api(projects.domain.models)
    // Exposed in VisaAuthTokenStorage (VisaAuthTokens)
    api(projects.domain.visa.models)
    implementation(projects.core.local)

    testImplementation(projects.test.core)
    testImplementation(deps.moshi.kotlin)
}