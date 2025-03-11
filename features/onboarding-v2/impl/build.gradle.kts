plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.onboarding.v2.impl"
}

dependencies {
    /** Api */
    implementation(projects.features.onboardingV2.api)
    implementation(projects.features.manageTokens.api)

    /** Core modules */
    implementation(projects.core.configToggles)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.res)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.datasource)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.feedback)
    implementation(projects.domain.core)
    implementation(projects.domain.card)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.legacy)
    implementation(projects.domain.settings)
    implementation(projects.domain.onboarding)
    implementation(projects.domain.visa)

    /** Tangem libraries */
    implementation(projects.libs.tangemSdkApi)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }

    /** AndroidX libraries */
    implementation(deps.androidx.core.ktx)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose libraries */
    implementation(deps.compose.material)  // to use buttons and text field in MultiWalletSeedPhraseImport.kt
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.coil)
    implementation(deps.lottie.compose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.datastore)

    /** Other libraries */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)
    implementation(deps.timber)
    implementation(deps.firebase.crashlytics)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}