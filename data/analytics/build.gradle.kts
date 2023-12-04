plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    id("configuration")
}

android {
    namespace = "com.tangem.data.analytics"
}

dependencies {

    /** Project - Analytics */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)

    /** Project - Data */
    implementation(projects.core.datasource)
    implementation(projects.data.common)

    /** AndroidX */
    implementation(deps.androidx.datastore)

    /** DI */
    implementation(deps.hilt.core)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.kotlin.coroutines)
    implementation(deps.timber)
}
