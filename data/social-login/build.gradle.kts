plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.sociallogin"
}

dependencies {
    implementation(projects.core.configToggles)

    api(projects.domain.socialLogin)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}