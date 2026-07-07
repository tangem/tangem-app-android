plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.txhistory.api"
}

dependencies {
    /** Kotlin */
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)

    /** Project - Core */
    api(projects.core.decompose)
    api(projects.core.ui)

    /** Domain models */
    api(projects.domain.models)
    api(projects.domain.txhistory)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
}