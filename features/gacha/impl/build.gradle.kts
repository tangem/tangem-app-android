plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.gacha.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.gacha.api)

    /** Project - Core */
    implementation(projects.core.configToggles)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}