plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.ksp)
    id("configuration")
}

android {
    namespace = "com.tangem.data.swap"
}

dependencies {
    /** Core */
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    /** Data */
    implementation(projects.data.common)
    implementation(projects.data.express)

    /** Domain */
    implementation(projects.domain.express.models)
    implementation(projects.domain.express)
    implementation(projects.domain.swap.models)
    implementation(projects.domain.swap)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)

    /** Tangem SDK */
    implementation(tangemDeps.blockchain) {
        exclude(module = "joda-time")
    }

    /** Libs */
    implementation(projects.libs.blockchainSdk)

    /** Other */
    implementation(deps.androidx.datastore)
    implementation(deps.jodatime)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}