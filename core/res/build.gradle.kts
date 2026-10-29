plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.core.res"
}

dependencies {

    // region AndroidX
    api(deps.androidx.annotation)
    // endregion

    // region Core modules
    implementation(projects.core.utils)

    testImplementation(projects.test.core)
    // endregion
}