plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.rating.impl"
}
dependencies {
    implementation(projects.features.rating.api)

    implementation(projects.core.decompose)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}