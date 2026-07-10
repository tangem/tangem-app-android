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
    api(projects.features.home.api)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.configToggles)
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.routing)

    /** Domain */
    api(projects.domain.appsflyer)
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.settings)
    api(projects.domain.wallets)
    implementation(projects.domain.models)

    /** Referral */
    api(projects.features.referral.domain)

    /** Compose libraries */
    api(deps.compose.animation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)

    /** Tangem libraries */
    implementation(tangemDeps.card.core)

    /** Other libraries */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
}