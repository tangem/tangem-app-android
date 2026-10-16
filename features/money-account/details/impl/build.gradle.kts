plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.moneyaccount.details.impl"
}

dependencies {
    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.feedback)
    implementation(projects.domain.feedback.models)
    implementation(projects.domain.models)
    implementation(projects.domain.moneyAccount)
    implementation(projects.domain.wallets)

    /** Features */
    implementation(projects.features.moneyAccount.details.api)
    implementation(projects.features.txhistory.api)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** Other */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)
    implementation(tangemDeps.blockchain)

    /** DI */
    implementation(deps.hilt.android)
    implementation(deps.androidx.appCompat)
    implementation(deps.haze)
    implementation(deps.kotlin.coroutines)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)
    kapt(deps.hilt.kapt)
}