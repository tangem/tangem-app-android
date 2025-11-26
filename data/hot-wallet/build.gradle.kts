plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.hotwallet"
}

dependencies {
    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(projects.domain.hotWallet)
    implementation(projects.domain.models)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.androidx.datastore)
    implementation(deps.kotlin.coroutines)
    implementation(deps.moshi)
    implementation(deps.moshi.kotlin)
}