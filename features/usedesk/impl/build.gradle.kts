plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.usedesk.impl"
}

dependencies {
    /** Core */
    api(projects.core.analytics)
    api(projects.core.datasource)
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.analytics.models)

    /** Features api */
    api(projects.features.usedesk.api)

    /** Domain */
    api(projects.domain.feedback)
    api(projects.domain.settings)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)

    /** AndroidX */
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.core)
    implementation(deps.androidx.fragment)
    implementation(deps.lifecycle.compose)
    implementation(deps.lifecycle.runtime.ktx)

    /** Other */
    implementation(deps.decompose)
    implementation(deps.kotlin.coroutines)

    /** Usedesk */
    implementation(tangemDeps.usedesk.chat.sdk)
    implementation(tangemDeps.usedesk.chat.gui)

    /** Tests */
    testImplementation(projects.test.core)
}