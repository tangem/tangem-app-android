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

    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    api(deps.retrofit)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    api(projects.core.remote)
    // TODO: temporary — needed only for EnvironmentConfig. Remove this dependency on core:datasource
    //  once per-stream environment config is extracted (e.g. a TangemPayEnvironmentConfig slice).
    api(projects.core.datasource)
    api(projects.core.utils)
    // Exposed in public store interface signatures (UserWalletId, TangemPayReissueCardFee,
    // TangemPayTxHistoryItem, ...)
    api(projects.domain.models)
    api(projects.domain.visa.models)
    implementation(projects.core.local)

    testImplementation(projects.test.core)
}