plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.virtualaccount.details.api"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain */
    api(projects.domain.models)

    /** Compose */
    implementation(deps.compose.runtime)
}