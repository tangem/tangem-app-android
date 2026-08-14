import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.visa"

    // `src/prodDi/` holds production DI bindings for TangemPay repos with a `mocked` counterpart.
    // Wired into every build type EXCEPT `mocked`, which supplies its own bindings from `src/mocked/`.
    buildTypes.configureEach {
        if (name != "mocked") {
            sourceSets.named(name) {
                java.srcDir("src/prodDi/kotlin")
            }
        }
    }
}
dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.moshi)
    implementation(deps.androidx.datastore)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.arrow.core)
    implementation(deps.arrow.fx)
    implementation(deps.jodatime)
    implementation(deps.okio)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    // endregion

    // region Libs - Tangem
    api(tangemDeps.hot.core)
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)
    // AndroidSecureStorageV2 for TangemPay secure token storage (see com.tangem.data.pay.store).
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.analytics)
    implementation(projects.core.local)
    api(projects.core.datasource)
    api(projects.spend.datasource)
    api(projects.core.security)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.error)
    implementation(projects.core.error.ext)
    implementation(projects.core.pagination)
    // endregion

    // region Project - Data
    api(projects.data.common)
    implementation(projects.data.wallets)
    // endregion

    // region Project - Domain
    api(projects.domain.common)
    api(projects.domain.core)
    api(projects.domain.networks)
    api(projects.domain.quotes)
    api(projects.domain.virtualAccount)
    api(projects.domain.visa)
    api(projects.domain.wallets)
    implementation(projects.domain.card)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.walletManager)
    runtimeOnly(projects.domain.tokens)
    // endregion

    // region Project - Domain models
    api(projects.domain.visa.models)
    implementation(projects.domain.appCurrency.models)
    // endregion

    // region Project - Features
    api(projects.features.swap.domain)
    api(projects.features.virtualAccounts.details.api)
    api(projects.features.tangempay.details.api)
    // endregion

    // region Project - Libs
    api(projects.libs.blockchainSdk)
    api(projects.libs.tangemSdkApi)
    implementation(projects.libs.visa)
    // endregion

    // region Test
    testImplementation(projects.test.core)
    testImplementation(deps.moshi.kotlin)
    // endregion
}