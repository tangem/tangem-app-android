plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.account.impl"
}

dependencies {
    /** Api */
    implementation(projects.features.account.api)

    /** Core modules */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.error)
    implementation(projects.core.res)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.datasource)

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.account)
    implementation(projects.domain.core)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** AndroidX libraries */
    implementation(deps.androidx.core.ktx)
    implementation(deps.lifecycle.runtime.ktx)

    /** Compose libraries */
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.coil)
    implementation(deps.decompose.ext.compose)
    implementation(deps.androidx.activity.compose)

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization)
    implementation(deps.timber)
    implementation(deps.firebase.crashlytics)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}