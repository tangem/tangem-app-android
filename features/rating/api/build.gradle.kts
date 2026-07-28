plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.rating.api"
}

dependencies {
    api(projects.core.decompose)
    api(projects.core.ui)
}