plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.marketing.api"
}

dependencies {
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.domain.marketing.models)

    implementation(deps.kotlin.coroutines)

    // The interface exposes a @Composable LinkedContent function, so the module needs the Compose compiler
    // (enabled via the module allowlist in the configuration convention plugin) and these APIs.
    api(deps.compose.runtime)
    api(deps.compose.ui)
}