plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.polymarket.impl"
}

dependencies {

    /** Feature */
    implementation(projects.features.polymarket.api)
    api(projects.features.commonFeatures.api)

    /** Core */
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Domain */
    implementation(projects.domain.account.status)
    implementation(projects.domain.common)
    implementation(projects.domain.markets)
    implementation(projects.domain.models)
    implementation(projects.domain.polymarket)

    /** Kotlin */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.runtime)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Decompose */
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
}