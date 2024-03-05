plugins {
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.android.library)
    id("configuration")
}

android {
    namespace = "com.tangem.data.source.preferences"
}

dependencies {
    implementation(deps.androidx.core.ktx)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    // For MoshiJsonConverter
    implementation(deps.tangem.card.core)
}