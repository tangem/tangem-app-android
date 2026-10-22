plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.libs.tangem_sdk_api"
}

dependencies {

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion

    // region AndroidX
    api(deps.androidx.annotation)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    implementation(deps.moshi)
    // endregion

    // region Tangem
    api(tangemDeps.card.core)
    // endregion

    // region Core modules
    api(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.local)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.visa.models)
    // endregion
}