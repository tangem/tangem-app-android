import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.features"
}

dependencies {
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.common)
    implementation(projects.libs.auth)
    implementation(projects.libs.blockchainSdk)
    implementation(projects.domain.core)
    implementation(projects.domain.demo)
    implementation(projects.domain.models)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.transaction.models)
    implementation(projects.domain.txhistory.models)
    implementation(projects.domain.wallets.models)
    /** Tangem libraries */
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.reKotlin)
    implementation(deps.timber)
    kaptForObfuscatingVariants(deps.moshi.kotlin.codegen)

    /** Testing libraries */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.truth)
    androidTestImplementation(deps.test.junit.android)
    androidTestImplementation(deps.test.espresso)
}