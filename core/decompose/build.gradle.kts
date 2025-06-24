plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.core.decompose"
}

dependencies {
    api(projects.core.utils)

    api(deps.decompose)
    api(deps.androidx.appCompat)
    implementation(deps.kotlin.coroutines)

    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)
}