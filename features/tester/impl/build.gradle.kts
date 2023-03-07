plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("configuration")
}

dependencies {
    /** AndroidX */
    implementation(AndroidX.activityCompose)

    /** Compose */
    implementation(Compose.foundation)
    implementation(Compose.hiltNavigation)
    implementation(Compose.material)
    implementation(Compose.navigation)
    implementation(Compose.ui)
    implementation(Compose.uiTooling)

    /** DI */
    implementation(Library.accompanistSystemUiController)
    implementation(Library.hilt)
    kapt(Library.hiltKapt)

    /** Core modules */
    implementation(project(":core:featuretoggles"))
    implementation(project(":core:ui"))

    /** Feature Apis */
    implementation(project(":features:tester:api"))
}
