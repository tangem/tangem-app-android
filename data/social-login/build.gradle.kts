plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.sociallogin"
}

dependencies {
    implementation(projects.core.configToggles)
    implementation(projects.core.local)
    implementation(projects.core.utils)
    implementation(projects.common.google)
    implementation(projects.domain.models)
    implementation(projects.libs.auth)

    api(projects.domain.socialLogin)

    implementation(deps.arrow.core)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    implementation(deps.androidx.datastore)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization)
    implementation(deps.kotlin.datetime)
    implementation(deps.okHttp)
    implementation(deps.retrofit)

    testImplementation(projects.test.core)
    testImplementation(deps.okio)
}