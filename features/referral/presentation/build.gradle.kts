plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("configuration")
}

dependencies {
    /** Core modules */
    implementation(project(":core:analytics"))
    implementation(project(":core:res"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))

    /** AndroidX */
    implementation(AndroidX.appCompat)
    implementation(AndroidX.fragmentKtx)
    implementation(AndroidX.lifecycleViewModelKtx)
    implementation(Library.materialComponent)

    /** Compose */
    implementation(Compose.foundation)
    implementation(Compose.material)
    implementation(Compose.uiTooling)

    /** Domain */
    implementation(project(":features:referral:domain"))

    /** Other libraries */
    implementation(Library.composeShimmer)
    implementation(Library.accompanistWebView)

    /** DI */
    implementation(Library.hilt)
    kapt(Library.hiltKapt)
}