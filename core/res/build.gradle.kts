plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.res"
}

dependencies {

    implementation(projects.core.utils)

    implementation(deps.timber)

    // region Firebase libraries
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)
    // endregion
}