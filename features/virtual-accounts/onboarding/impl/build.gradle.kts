plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.virtualaccount.onboarding.impl"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.error)
    implementation(projects.core.ui)

    /** Common */
    api(projects.common.routing)

    /** Api */
    api(projects.features.virtualAccounts.onboarding.api)

    /** Domain */
    api(projects.domain.common)
    api(projects.domain.visa)
    implementation(projects.domain.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)
}