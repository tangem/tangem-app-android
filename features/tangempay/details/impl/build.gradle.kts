plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tangempay.details.impl"
}

dependencies {
    /** Core */
    api(projects.core.analytics)
    api(projects.core.configToggles)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)
    implementation(projects.core.error)
    implementation(projects.core.pagination)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Features api */
    api(projects.features.tangempay.details.api)
    api(projects.features.tokenRecieve.api)
    api(projects.features.tokendetails.api)
    api(projects.features.promoBanners.api)
    api(projects.features.virtualAccounts.details.api) // TWI_1638_VA_MVP0_ENABLED

    /** Domain */
    api(projects.domain.balanceHiding)
    api(projects.domain.feedback)
    api(projects.domain.models)
    api(projects.domain.visa)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.onramp.models)
    implementation(projects.domain.visa.models)
    runtimeOnly(projects.domain.txhistory)
    runtimeOnly(projects.domain.wallets)

    /** Compose */
    api(deps.compose.coil)
    api(deps.compose.foundation)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)
    implementation(deps.lottie)
    implementation(deps.lottie.compose)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
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
    implementation(deps.haze)
    implementation(deps.jodatime)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** Test */
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}