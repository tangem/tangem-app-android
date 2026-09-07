plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.google"
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    implementation(projects.core.utils)
    implementation(projects.core.local)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    implementation(deps.androidx.activity)

    implementation(deps.okHttp)
    implementation(deps.retrofit)
    implementation(deps.retrofit.kotlinxSerialization)

    implementation(deps.googlePlay.services.wallet)
    implementation(deps.googlePlay.services.auth)

    testImplementation(projects.test.core)
}