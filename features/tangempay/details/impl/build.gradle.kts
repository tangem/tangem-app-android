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
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.error)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.ui)
    implementation(projects.common.routing)

    /** Features api */
    implementation(projects.features.tangempay.details.api)
    implementation(projects.features.tokenRecieve.api)
    implementation(projects.features.txhistory.api)
    implementation(projects.features.tokendetails.api)

    /** Domain */
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.feedback)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.models)
    implementation(projects.domain.visa)
    implementation(projects.domain.visa.models)
    implementation(projects.domain.wallets)

    /** Compose */
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)
    implementation(deps.lottie.compose)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)
}