plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.core.deeplink"
}

dependencies {

    /* Libs - AndroidX */
    implementation(deps.lifecycle.runtime.ktx)

    /* Libs - Other */
    implementation(deps.timber)

    /* DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}