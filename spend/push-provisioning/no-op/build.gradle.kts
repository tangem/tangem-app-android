plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.spend.pushProvisioning.noop"
}

dependencies {
    api(projects.spend.pushProvisioning.api)

    implementation(deps.kotlin.coroutines)

    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    testImplementation(projects.test.core)
}