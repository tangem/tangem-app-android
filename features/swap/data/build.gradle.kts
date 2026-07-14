import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.swap.data"
}
dependencies {

    /** AndroidX */
    implementation(deps.androidx.datastore)

    /** Project*/
    api(projects.core.datasource)
    api(projects.core.utils)
    api(projects.features.swap.domain)
    implementation(projects.domain.express.models)

    /** Network */
    api(deps.moshi)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    /** Domain */
    api(projects.domain.account)
    api(projects.domain.models)
    api(projects.domain.txhistory)
    runtimeOnly(projects.domain.account.status)
    runtimeOnly(projects.domain.wallets)

    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)

    /** Data */
    api(projects.data.common)

    /** Tangem SDKs */
    implementation(tangemDeps.blockchain)

    /** Others */
    implementation(deps.jodatime)

    /** DI */
    implementation(deps.hilt.android)

    kapt(deps.hilt.kapt)

    /** Test */
    testImplementation(projects.test.core)
}