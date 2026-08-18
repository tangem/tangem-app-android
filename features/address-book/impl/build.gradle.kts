plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.addressbook.impl"
}

dependencies {
    /** Api */
    api(projects.features.addressBook.api)
    api(projects.features.commonFeatures.api)

    /** Domain */
    api(projects.domain.addressBook)
    api(projects.domain.wallets)
    implementation(projects.domain.models)
    implementation(projects.domain.qrScanning)
    implementation(projects.domain.qrScanning.models)

    /** Common */
    implementation(projects.common.routing)
    implementation(projects.common.ui)

    /** Core modules */
    api(projects.core.decompose)
    api(projects.core.ui)
    api(projects.core.utils)
    api(projects.core.analytics)
    api(projects.core.analytics.models)
    api(projects.domain.common)
    implementation(projects.core.navigation)

    /** Compose */
    api(deps.compose.foundation)
    implementation(deps.compose.animation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    api(deps.kotlin.immutable.collections)
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.coroutines)
    implementation(deps.kotlin.serialization.core)
    implementation(deps.haze)

    /** Utils */
    implementation(projects.libs.blockchainSdk)
    implementation(tangemDeps.blockchain)
    implementation(tangemDeps.card.core)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(projects.common.test)
    testImplementation(projects.domain.transaction)
}