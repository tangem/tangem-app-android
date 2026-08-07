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
    api(projects.features.biometry.api)
    api(projects.features.hotWallet.api)
    api(projects.features.manageTokens.api)
    api(projects.features.onboardingV2.api)
    api(projects.features.pushNotifications.api)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.error)
    implementation(projects.core.error.ext)

    /** Common */
    api(projects.common.ui)
    implementation(projects.common.routing)
    implementation(projects.common)

    /** Domain */
    api(projects.domain.account)
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.feedback)
    api(projects.domain.models)
    api(projects.domain.networks)
    api(projects.domain.onboarding)
    api(projects.domain.settings)
    api(projects.domain.staking)
    api(projects.domain.tokens)
    api(projects.domain.visa)
    api(projects.domain.wallets)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.visa.models)
    implementation(projects.domain.wallets.models)
    runtimeOnly(projects.domain.onramp)
    runtimeOnly(projects.domain.transaction)

    /** Tangem libraries */
    api(tangemDeps.card.core)
    api(projects.libs.tangemSdkApi)
    implementation(tangemDeps.hot.core)
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }

    /** AndroidX libraries */
    api(deps.androidx.appCompat)
    implementation(deps.androidx.annotation)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose libraries */
    api(deps.compose.animation)
    api(deps.compose.foundation)
    api(deps.decompose.ext.compose)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.coil)
    implementation(deps.lottie)
    implementation(deps.lottie.compose)
    implementation(deps.androidx.activity.compose)

    /** Other libraries */
    api(deps.kotlin.coroutines)
    api(deps.kotlin.immutable.collections)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.serialization)
    implementation(deps.firebase.crashlytics)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Test */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.coroutine)
}