plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.appsflyer"
}

dependencies {
    implementation(projects.core.datasource)

    implementation(projects.domain.appsflyer)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}