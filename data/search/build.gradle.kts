plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.search"
}

dependencies {
    // region Project - Core
    implementation(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Project - Data
    implementation(projects.data.common)
    // endregion

    // region Project - Domain
    implementation(projects.domain.search)
    implementation(projects.domain.common)
    implementation(projects.domain.account.status)
    implementation(projects.domain.markets.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.appCurrency)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Other libraries
    implementation(deps.androidx.datastore)
    implementation(deps.moshi.kotlin)
    // endregion
}