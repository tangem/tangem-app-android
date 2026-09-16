plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.collectibles.api"
}

dependencies {
    /** Compose */
    api(deps.compose.foundation)
    api(deps.compose.ui)
    implementation(deps.compose.runtime)

    /** Project - Common */
    api(projects.common.routing)

    /** Project - Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)

    /** Project - Domain */
    api(projects.domain.models)
}