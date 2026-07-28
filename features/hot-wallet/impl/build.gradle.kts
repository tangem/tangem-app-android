plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.hotwallet.impl"
}
dependencies {
    /** Api */
    api(projects.features.hotWallet.api)
    api(projects.features.onboardingV2.api)
    api(projects.features.pushNotifications.api)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.configToggles)
    api(projects.core.datasource)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.res)

    /** Domain */
    api(projects.domain.assetsdiscovery)
    api(projects.domain.card)
    api(projects.domain.common)
    api(projects.domain.feedback)
    api(projects.domain.hotWallet)
    api(projects.domain.settings)
    api(projects.domain.wallets)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.models)
    implementation(projects.domain.wallets.models)

    /** Common */
    implementation(projects.common.routing)
    runtimeOnly(projects.common.ui)

    /** Tangem libraries */
    api(projects.libs.tangemSdkApi)
    api(tangemDeps.hot.core)
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.card.android) {
        exclude(module = "joda-time")
    }
    runtimeOnly(tangemDeps.hot.android)

    /** AndroidX libraries */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose libraries */
    api(deps.compose.animation)
    implementation(deps.compose.material3)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.lottie)
    implementation(deps.lottie.compose)
    implementation(deps.decompose.ext.compose)
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