plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.referral.data"
}

dependencies {

    /** Project */
    api(projects.core.datasource)
    api(projects.core.utils)

    /** Data modules */
    implementation(projects.data.common)
    implementation(deps.androidx.datastore)

    /** Domain modules */
    api(projects.domain.common)
    api(projects.domain.referral)
    api(projects.features.referral.domain)
    api(projects.libs.blockchainSdk)
    implementation(projects.domain.models)

    /** Libs */
    runtimeOnly(projects.libs.auth)

    /** Time */
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tangem deps */
    implementation(tangemDeps.blockchain)
}