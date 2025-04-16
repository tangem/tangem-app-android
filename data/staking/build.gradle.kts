import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.staking"
}

dependencies {
    /** Core modules */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Common modules */
    implementation(projects.data.common)

    /** Domain modules */
    implementation(projects.core.configToggles)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)

    /** Feature Api modules */
    implementation(projects.features.staking.api)

    // region DI

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    // endregion

    // region Others dependencies

    implementation(deps.androidx.datastore)
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)
    implementation(deps.firebase.crashlytics)
    ksp(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)

    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)

    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    // endregion

    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(tangemDeps.card.core)
    testImplementation(projects.common.test)
}
