plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.welcome.api"
}

dependencies {
    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.common.routing)

    /** Domain models */
    implementation(projects.domain.wallets.models)

    /** Compose */
    implementation(deps.compose.runtime)
}