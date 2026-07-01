plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.appcurrency"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Project - Data
    api(projects.data.common)
    // endregion

    // region Project - Domain
    api(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    // endregion
}