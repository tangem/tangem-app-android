plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("configuration")
}

dependencies {
    /** Core modules */
    implementation(project(":core:analytics"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))

    /** AndroidX */
    implementation(AndroidX.activityCompose)
    implementation(AndroidX.appCompat)
    implementation(AndroidX.fragmentKtx)
    implementation(AndroidX.lifecycleViewModelKtx)
    implementation(AndroidX.browser)

    /** Compose */
    implementation(Compose.foundation)
    implementation(Compose.material)
    implementation(Compose.uiTooling)
    implementation(Compose.coil)
    implementation(Compose.constraintLayout)

    /** Domain */
    implementation(project(":features:swap:domain"))

    /** Other libraries */
    implementation(Library.composeShimmer)
    implementation(Library.kotlinSerialization)
    implementation(Library.timber)

    /** DI */
    implementation(Library.hilt)
    kapt(Library.hiltKapt)
}
