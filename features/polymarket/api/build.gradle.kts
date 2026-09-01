plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.polymarket.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.models)

    /** Compose */
    api(deps.compose.foundation)
    api(deps.compose.ui)
    implementation(deps.compose.runtime)
}