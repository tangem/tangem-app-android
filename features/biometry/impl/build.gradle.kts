plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.biometry.impl"
}

dependencies {
    api(projects.features.biometry.api)

    /** Core modules */
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.ui)

    /** Domain */
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.settings)
    api(projects.domain.wallets)
    implementation(projects.domain.models)

    /** Compose libraries */
    api(deps.compose.animation)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Tangem libraries */
    api(projects.libs.tangemSdkApi)

    /** Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}