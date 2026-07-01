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
    implementation(projects.features.virtualAccounts.details.api)

    implementation(projects.core.configToggles)

    implementation(deps.compose.runtime)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}