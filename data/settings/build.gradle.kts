plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.settings"
}

dependencies {

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.kotlin.coroutines)

    implementation(projects.core.utils)

    implementation(projects.domain.settings)

    implementation(projects.data.source.preferences)
}
