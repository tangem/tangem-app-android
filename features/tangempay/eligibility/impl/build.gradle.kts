plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.tangempay.eligibility.impl"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)

    /** Common */
    implementation(projects.common.routing)

    /** Features api */
    api(projects.features.tangempay.eligibility.api)
    implementation(projects.features.tangempay.commonUi)

    /** Domain */
    implementation(projects.domain.models)

    /** Compose */
    api(deps.compose.coil)
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    api(deps.kotlin.coroutines)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)

    /** Test */
    testImplementation(projects.test.core)
    testImplementation(deps.test.junit5)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    testImplementation(deps.test.coroutine)
}