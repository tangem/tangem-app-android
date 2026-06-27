plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.txhistory.api"
}

dependencies {
    /** Project - Core */
    implementation(projects.core.ui)
    implementation(projects.core.decompose)

    /** Domain models */
    api(projects.domain.models)
    api(projects.domain.txhistory)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.wallets.models)

    /** Compose */
    implementation(deps.compose.runtime)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.kotlin.immutable.collections)
}