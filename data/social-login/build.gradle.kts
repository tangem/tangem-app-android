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
    implementation(projects.core.utils)
    implementation(projects.common.google)

    api(projects.domain.socialLogin)

    implementation(deps.arrow.core)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    testImplementation(projects.test.core)
}