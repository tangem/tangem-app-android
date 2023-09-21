plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.common"
}

dependencies {
    implementation(projects.core.datasource)

    implementation(deps.kotlin.coroutines)
    implementation(deps.jodatime)
    implementation(deps.timber)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}
