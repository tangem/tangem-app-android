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
    api(projects.core.analytics)
    implementation(projects.core.analytics.models)
    api(projects.core.decompose)
    api(projects.core.navigation)
    api(projects.core.ui)
    api(projects.core.utils)

    /** Common */
    implementation(projects.common.routing)

    /** Features api */
    api(projects.features.tangempay.eligibility.api)
    implementation(projects.features.commonFeatures.api)

    /** Domain */
    implementation(projects.domain.models)
    implementation(projects.domain.visa)
    implementation(projects.domain.visa.models)

    /** Compose */
    api(deps.compose.coil)
    api(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** AndroidX */
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.appCompat)
    implementation(deps.lifecycle.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    api(deps.kotlin.coroutines)
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.kotlin.serialization.core)
    runtimeOnly(deps.room.runtime)

    /** Test */
    testImplementation(projects.test.core)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.coroutine)
}