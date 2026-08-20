plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.storiesv2.impl"
}

dependencies {
    /** Core */
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.ui)

    /** Api */
    api(projects.features.storiesV2.api)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.androidx.annotation)
    implementation(deps.kotlin.coroutines)

    /** Tests */
    testImplementation(projects.test.core)
}