plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.virtualaccount.main.impl"
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.res)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.ui)

    /** Features api */
    implementation(projects.features.virtualAccounts.main.api)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}