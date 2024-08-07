plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.markets"
}

dependencies {
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.core.pagination)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.markets)
    implementation(projects.data.common)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Others dependencies
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.timber)

    implementation(projects.libs.blockchainSdk)
    // endregion
}
