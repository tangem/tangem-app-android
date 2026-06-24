plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.virtualaccount.details.impl"
}

dependencies {
    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.models)

    /** Features */
    implementation(projects.features.virtualAccounts.details.api)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}