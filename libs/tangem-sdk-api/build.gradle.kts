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
    api(deps.androidx.activity)
    api(deps.androidx.annotation)
    // endregion

    // region Other libraries
    api(deps.arrow.core)
    // endregion

    // region Tangem
    api(tangemDeps.card.core)
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }
    // endregion

    // region Core modules
    api(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    // endregion

    // region Domain models
    api(projects.domain.models)
    api(projects.domain.visa.models)
    // endregion
}