plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.onboarding"
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

    // region Core modules
    implementation(projects.store.datasource)
    implementation(projects.core.local)
    implementation(projects.core.utils)
    // endregion

    // region Domain modules
    api(projects.domain.onboarding)
    implementation(projects.domain.models)
    // endregion
}