plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tangempay.onboarding.impl"
}
dependencies {
    /** Core */
    api(projects.core.analytics)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.error)
    implementation(projects.core.ui)

    /** Common */
    api(projects.common.routing)
    implementation(projects.common.ui)

    /** Features api */
    api(projects.features.tangempay.onboarding.api)
    api(projects.features.wallet.api)
    implementation(projects.features.tangempay.details.api)

    /** Domain */
    api(projects.domain.appsflyer)
    api(projects.domain.hotWallet)
    api(projects.domain.visa)
    api(projects.domain.visa.models)
    api(projects.domain.wallets)
    implementation(projects.domain.models)

    /** Libs */
    implementation(tangemDeps.hot.core)

    /** Data **/
    runtimeOnly(projects.data.visa)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** AndroidX */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    api(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.jodatime)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** Test */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}