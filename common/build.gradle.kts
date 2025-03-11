plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common"
}

dependencies {

    implementation(projects.core.utils)

    // region Firebase libraries
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    implementation(deps.firebase.messaging)
    // end

    implementation(deps.timber)

    implementation(deps.arrow.core)

    implementation(deps.test.junit)
    implementation(deps.test.truth)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // end
}