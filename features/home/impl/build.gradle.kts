plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.home.impl"
}

dependencies {
    /** Api */
    implementation(projects.features.home.api)

    /** Core modules */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    implementation(projects.core.configToggles)

    /** Common */
    implementation(projects.common.routing)

    /** Domain */
    implementation(projects.domain.common)
    implementation(projects.domain.models)
    implementation(projects.domain.card)
    implementation(projects.domain.settings)
    implementation(projects.domain.wallets)

    /** Referral */
    implementation(projects.features.referral.domain)

    /** Compose libraries */
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)

    /** Tangem libraries */
    implementation(tangemDeps.card.core)

    /** Other libraries */
    implementation(deps.kotlin.immutable.collections)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
}