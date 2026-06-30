plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.foryou.impl"
}

dependencies {

    /** Features */
    implementation(projects.features.forYou.api)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.configToggles)

    implementation(deps.compose.ui)
    implementation(deps.compose.foundation)
    implementation(deps.lifecycle.compose)
    implementation(deps.compose.material3)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}