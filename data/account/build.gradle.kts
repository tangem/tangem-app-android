plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.account"
}

dependencies {

    // region Project - Core
    api(projects.core.utils)
    // endregion

    // region Project - Domain
    api(projects.domain.account)
    api(projects.domain.models)
    // endregion

    // Project - Data
    implementation(projects.core.datasource)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Other Dependencies
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.timber)
    // endregion
}