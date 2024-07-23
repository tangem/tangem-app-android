plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
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
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.wallets.models)

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
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)


    implementation(projects.libs.blockchainSdk)
    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }

    // endregion
}

