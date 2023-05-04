plugins {
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.android.library)
    id("configuration")
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