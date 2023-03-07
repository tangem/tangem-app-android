plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("configuration")
}

dependencies {

    /** DI */
    implementation(Library.hilt)
    kapt(Library.hiltKapt)

    /** Core shouldn't depends on core, but in case with utils and logging its necessary */
    implementation(project(":core:utils"))
}
