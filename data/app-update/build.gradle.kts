plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.data.appupdate"
}

dependencies {

    implementation(projects.core.datasource)
    implementation(projects.core.utils)

    implementation(projects.domain.appUpdate)

    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)

    // region DI
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
    // endregion
}