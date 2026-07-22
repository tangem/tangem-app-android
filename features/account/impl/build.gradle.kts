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
    api(projects.features.account.api)

    /** Core modules */
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.error)
    api(projects.core.utils)
    implementation(projects.core.ui)
    implementation(projects.core.res)

    /** Domain */
    api(projects.domain.account)
    api(projects.domain.account.status)
    api(projects.domain.models)
    api(projects.domain.wallets)
    implementation(projects.domain.core)
    runtimeOnly(projects.domain.appCurrency)
    runtimeOnly(projects.domain.balanceHiding)
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
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)
    implementation(deps.androidx.activity.compose)

    /** Other libraries */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.firebase.crashlytics)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)
}