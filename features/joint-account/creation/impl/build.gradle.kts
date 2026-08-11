plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.jointaccount.creation.impl"
}

dependencies {
    /** Core */
    api(projects.core.decompose)
    api(projects.core.utils)
    implementation(projects.core.ui)

    /** Api */
    api(projects.features.jointAccount.creation.api)

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
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.lifecycle.compose)
}