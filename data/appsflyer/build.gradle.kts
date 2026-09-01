plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.ksp)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.appsflyer"
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    api(projects.core.datasource)
    implementation(projects.core.local)
    // endregion

    // region Domain
    api(projects.domain.appsflyer)
    implementation(projects.domain.wallets.models)
    // endregion

    // region Other
    api(deps.moshi)
    ksp(deps.moshi.kotlin.codegen)
    implementation(deps.kotlin.coroutines)
    // endregion
}