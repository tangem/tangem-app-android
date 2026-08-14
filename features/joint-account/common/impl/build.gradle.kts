plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.jointaccount.common.impl"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.ui)

    /** Api */
    api(projects.features.jointAccount.common.api)

    /** Common */
    implementation(projects.common.ui)

    /** Domain */
    implementation(projects.domain.common)
    implementation(projects.domain.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.decompose.ext.compose)

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Other */
    implementation(deps.androidx.annotation)
    implementation(deps.androidx.appCompat)
    implementation(deps.kotlin.coroutines)
    implementation(deps.lifecycle.compose)

    /** Tests */
    testImplementation(projects.test.core)
}