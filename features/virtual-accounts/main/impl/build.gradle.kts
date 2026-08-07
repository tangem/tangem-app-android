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
    api(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Common */
    implementation(projects.common.ui)

    /** Features api */
    implementation(projects.features.virtualAccounts.main.api)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}