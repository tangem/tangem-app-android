plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.home.api"
}

dependencies {
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.routing)
} 