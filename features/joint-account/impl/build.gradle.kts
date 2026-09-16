plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.jointaccount.impl"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)

    /** Api */
    api(projects.features.jointAccount.api)

    /** Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Features */
    implementation(projects.features.commonFeatures.api)
    implementation(projects.features.pushNotifications.api)
    implementation(projects.features.wallet.api)

    /** Domain */
    implementation(projects.domain.account)
    implementation(projects.domain.account.status)
    implementation(projects.domain.common)
    implementation(projects.domain.jointAccount)
    implementation(projects.domain.models)
    implementation(projects.domain.notifications.models)
    implementation(projects.domain.wallets)

    /** SDK */
    implementation(projects.libs.blockchainSdk)
    implementation(tangemDeps.blockchain)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.haze)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)
    implementation(deps.lifecycle.compose)

    /** Tests */
    testImplementation(projects.common.test)
    testImplementation(projects.test.core)
    testImplementation(projects.test.mock)
}