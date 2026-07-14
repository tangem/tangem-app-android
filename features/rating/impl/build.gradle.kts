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
    api(projects.features.rating.api)

    api(projects.core.decompose)
    api(projects.core.ui)
    api(projects.core.utils)

    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)

    implementation(deps.androidx.appCompat)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}