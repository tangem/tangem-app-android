import com.tangem.plugin.configuration.configurations.extension.kaptForObfuscatingVariants

plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.onramp"
}

dependencies {
    /** Core modules */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.core.deepLinks.global)
    implementation(projects.core.analytics)

    /** Common modules */
    implementation(projects.data.common)

    /** Domain modules */
    implementation(projects.domain.onramp)
    implementation(projects.domain.legacy)
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.models)

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
    kaptForObfuscatingVariants(deps.moshi.kotlin.codegen)
    kaptForObfuscatingVariants(deps.retrofit.response.type.keeper)
    implementation(deps.kotlin.serialization)


    implementation(projects.libs.blockchainSdk)
    implementation(projects.libs.crypto)

    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    // endregion
}
