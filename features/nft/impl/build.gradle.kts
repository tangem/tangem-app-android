plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.nft.impl"
}

dependencies {
    /** Api */
    api(projects.features.commonFeatures.api)
    api(projects.features.nft.api)
    api(projects.features.tokenRecieve.api)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)

    /** Domain modules */
    api(projects.domain.card)
    api(projects.domain.feedback)
    api(projects.domain.account)
    api(projects.domain.account.status)
    api(projects.domain.appCurrency)
    api(projects.domain.nft)
    api(projects.domain.transaction)
    api(projects.domain.wallets)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.core)
    implementation(projects.domain.models)
    implementation(projects.domain.nft.models)
    runtimeOnly(projects.domain.tokens)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** AndroidX libraries */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose libraries */
    api(deps.compose.animation)
    api(deps.compose.coil)
    api(deps.compose.foundation)
    api(deps.decompose.ext.compose)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
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
}