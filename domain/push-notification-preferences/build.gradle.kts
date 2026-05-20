plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.domain.pushnotificationpreferences"
}

dependencies {
    /** Domain */
    implementation(projects.domain.models)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}