plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.introduction.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.hotWallet.api)
    implementation(projects.features.introduction.api)
    implementation(projects.features.pushNotifications.api)
    implementation(projects.features.storiesV2.api)

    /** Core */
    implementation(projects.common.routing)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.analytics)
    implementation(projects.domain.appsflyer)
    implementation(projects.domain.card)
    implementation(projects.domain.cloudBackup)
    implementation(projects.domain.common)
    implementation(projects.domain.hotWallet)
    implementation(projects.domain.models)
    implementation(projects.domain.settings)
    implementation(projects.domain.socialLogin)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.lifecycle.compose)

    /** Media */
    implementation(deps.androidx.media3.common)
    implementation(deps.androidx.media3.datasource)
    implementation(deps.androidx.media3.exoplayer)

    /** SDK */
    implementation(tangemDeps.card.core)
    implementation(tangemDeps.hot.core)

    /** Other */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.annotation.experimental)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(deps.arrow.core)
    testImplementation(deps.kotlin.serialization.core)
    testImplementation(projects.test.core)
}