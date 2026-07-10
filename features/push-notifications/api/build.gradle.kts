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
    implementation(deps.androidx.annotation)

    /** Core */
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Common */
    api(projects.common.routing)
}