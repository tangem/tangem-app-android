plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.promobanners.api"
}

dependencies {
    api(deps.compose.runtime)
    api(deps.compose.ui)

    api(projects.core.decompose)
    api(projects.core.ui)
}