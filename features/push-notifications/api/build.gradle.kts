plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.pushnotifications.api"
}

dependencies {
    /** AndroidX */
    implementation(deps.androidx.fragment.ktx)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.analytics.models)

    /** Common */
    implementation(projects.common.routing)
}