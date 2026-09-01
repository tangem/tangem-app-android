plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.introduction.api"
}

dependencies {
    /** Core */
    api(projects.common.routing)
    api(projects.core.decompose)
    api(projects.core.ui)
}