plugins {
    id("com.android.library")
    kotlin("android")
    id("configuration")
}

dependencies {
    /** AndroidX libraries */
    implementation(AndroidX.fragmentKtx)

    /** Compose */
    implementation(Compose.foundation)
    implementation(Compose.material)
    implementation(Compose.uiTooling)

    /** Other libraries */
    implementation(Library.accompanistSystemUiController)
    implementation(Library.materialComponent)
    implementation(Library.composeShimmer)

    implementation(project(":core:res"))
}
