plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.allservices.impl"
}

dependencies {
    /** Project - API */
    api(projects.features.allServices.api)

    /** Common */
    implementation(projects.common.routing)

    /** Core */
    implementation(projects.core.analytics)
    implementation(projects.core.configToggles)
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Features */
    implementation(projects.features.commonFeatures.api)
    implementation(projects.features.introduction.api)
    implementation(projects.features.jointAccount.api)

    /** Domain */
    implementation(projects.domain.card)
    implementation(projects.domain.common)
    implementation(projects.domain.demo)
    implementation(projects.domain.wallets)
    implementation(projects.domain.earn)
    implementation(projects.domain.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.lifecycle.compose)

    /** DI */
    implementation(deps.decompose)
    implementation(deps.decompose.ext.compose)
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
}