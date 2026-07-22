plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.express"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    api(deps.moshi)
    implementation(deps.arrow.core)
    // endregion

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core
    api(projects.core.datasource)
    api(projects.core.utils)
    // endregion

    // region Data
    implementation(projects.data.common)
    // endregion

    // region Domain
    api(projects.domain.common)
    api(projects.domain.express)
    api(projects.domain.express.models)
    api(projects.domain.txhistory)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    // endregion
}