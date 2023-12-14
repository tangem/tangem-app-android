plugins {
    alias(deps.plugins.android.library)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.hilt.android)
    id("configuration")
}

android {
    namespace = "com.tangem.feature.qrscanning.impl"

    viewBinding {
        enable = true
    }
}


dependencies {

    implementation(deps.androidx.fragment.ktx)
    implementation(deps.viewBindingDelegate)

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

    implementation(projects.features.qrScanning.api)

    /** Compose */
    implementation(deps.compose.foundation)

    implementation(deps.arrow.core)
}