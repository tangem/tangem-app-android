plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    id("configuration")
}

android {
    namespace = "com.tangem.features.payment.impl"
}

dependencies {
    /** Core */
    implementation(projects.core.ui)

    /** Common */
    implementation(projects.common.ui)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.material3)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling.preview)
    debugImplementation(deps.compose.ui.tooling)

    /** Other */
    implementation(deps.kotlin.immutable.collections)
}