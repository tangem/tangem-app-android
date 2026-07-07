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
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.ui)

}