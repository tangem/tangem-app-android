plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.usedesk.api"
}

dependencies {

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

}