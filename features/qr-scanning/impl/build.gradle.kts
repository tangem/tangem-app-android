plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.qrscanning.impl"
}

dependencies {

    /** Core */
    implementation(projects.core.ui)
    implementation(projects.core.utils)
    implementation(projects.core.navigation)
    implementation(projects.common.routing)

    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.activity.compose)
    implementation(deps.lifecycle.compose)

    /** Camera */
    implementation(deps.camera.camera2)
    implementation(deps.camera.lifecycle)
    implementation(deps.camera.view)

    implementation(deps.listenableFuture)
    implementation(deps.mlKit.barcodeScanning)

    /** Excluded dependencies */
    implementation("com.google.guava:guava:30.0-android") {
        // excludes version 9999.0-empty-to-avoid-conflict-with-guava
        exclude(group="com.google.guava", module = "listenablefuture")
    }

    /** DI */
    implementation(deps.hilt.android)
    kapt(deps.hilt.kapt)

    /** Api */
    implementation(projects.features.qrScanning.api)

    /** Domain */
    implementation(projects.domain.qrScanning)
    implementation(projects.domain.qrScanning.models)

    /** Compose */
    implementation(deps.compose.foundation)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.material3)
    implementation(deps.compose.accompanist.systemUiController)

    /** Other dependencies */
    implementation(deps.arrow.core)
    implementation(deps.timber)
}