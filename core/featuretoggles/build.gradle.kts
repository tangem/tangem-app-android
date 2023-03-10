plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

dependencies {
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    implementation(deps.timber)

    implementation(project(":core:datasource"))
}