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
    implementation(projects.features.hotWallet.api)

    /** Core modules */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.res)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.navigation)
    
    /** Common */
    implementation(projects.common.routing)
    
    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.core)
    implementation(projects.domain.card)
    implementation(projects.domain.settings)
    implementation(projects.domain.tokens)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.feedback)
    implementation(projects.domain.feedback.models)

    /** AndroidX libraries */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.runtime.ktx)
    
    /** Compose libraries */
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.coil)
    implementation(deps.decompose.ext.compose)
    
    /** Tangem libraries */
    implementation(tangemDeps.card.android)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.blockchain)
    
    /** Other libraries */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
    
    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
} 