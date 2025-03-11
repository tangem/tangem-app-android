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

    // region AndroidX libraries
    implementation(deps.androidx.datastore)
    // endregion

    // region DI
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
    // endregion

    // region Core modules
    implementation(projects.core.datasource)
    // endregion

    // region Domain modules
    implementation(projects.domain.onboarding)
    implementation(projects.domain.models)
    // endregion
}