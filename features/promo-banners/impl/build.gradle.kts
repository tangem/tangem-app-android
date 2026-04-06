plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.promobanners.impl"
}

dependencies {
    /** Project - API */
    implementation(projects.features.promoBanners.api)

    /** Domain */
    implementation(projects.domain.common)
    implementation(projects.domain.models)

    /** Core */
    implementation(projects.core.decompose)
    implementation(projects.core.ui)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.utils)
    implementation(projects.core.datasource)
    implementation(projects.core.configToggles)

    /** Common */
    implementation(projects.common.routing)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.lifecycle.compose)

    /** Other */
    implementation(deps.arrow.core)
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.timber)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Tests */
    testImplementation(deps.test.junit5)
    testRuntimeOnly(deps.test.junit5.engine)
    testImplementation(deps.test.truth)
}