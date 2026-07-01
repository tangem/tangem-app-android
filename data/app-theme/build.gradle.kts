plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.apptheme"
}

dependencies {

    // region Kotlin
    implementation(deps.kotlin.coroutines)
    // endregion

    // region Other libraries
    implementation(deps.androidx.datastore)
    implementation(deps.moshi)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Project - Core
    api(projects.core.datasource)
    implementation(projects.core.utils)
    // endregion

    // region Project - Domain
    api(projects.domain.appTheme)
    implementation(projects.domain.appTheme.models)
    // endregion
}