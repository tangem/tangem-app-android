plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.common.ui.markets"
}

dependencies {
    /** Project - Core */
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.navigation)
    implementation(projects.core.analytics)

    /** Project - Common */
    implementation(projects.common.uiCharts)
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Project - Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.staking)
    implementation(projects.domain.staking.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.offramp)
    implementation(projects.domain.demo)

    implementation(deps.lifecycle.compose)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.ui.utils)
    implementation(deps.kotlin.immutable.collections)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}