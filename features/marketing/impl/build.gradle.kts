plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.marketing.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.marketing.api)

    /** Domain */
    implementation(projects.domain.marketing)
    implementation(projects.domain.marketing.models)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.navigation)
    implementation(projects.core.ui)
    implementation(projects.core.utils)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.compose.coil)
    implementation(deps.lifecycle.compose)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.coroutines)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(projects.test.core)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.turbine)
    testImplementation(deps.test.coroutine)
}