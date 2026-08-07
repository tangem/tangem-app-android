plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    id("configuration")
}

android {
    namespace = "com.tangem.features.managetokens.api"
}

dependencies {
    /* Kotlin */
    api(deps.kotlin.serialization.core)

    /* Project - Domain */
    api(projects.domain.models)

    /* Project - Core */
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.ui)

    /* Compose */
    implementation(deps.compose.runtime)
}