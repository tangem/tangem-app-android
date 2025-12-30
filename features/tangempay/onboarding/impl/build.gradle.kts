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
    implementation(projects.core.analytics)
    implementation(projects.core.decompose)
    implementation(projects.core.error)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Features api */
    implementation(projects.features.tangempay.onboarding.api)
    implementation(projects.features.tangempay.details.api)
    implementation(projects.features.kyc.api)
    implementation(projects.features.wallet.api)
    implementation(projects.features.hotWallet.api)

    /** Domain */
    implementation(projects.domain.visa)
    implementation(projects.domain.wallets)

    /** Data **/
    implementation(projects.data.visa)

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
    implementation(deps.timber)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
}