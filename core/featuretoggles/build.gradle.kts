plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("configuration")
}

dependencies {
    implementation(Library.moshi)
    implementation(Library.moshiKotlin)
    implementation(Library.hilt)
    kapt(Library.hiltKapt)
    implementation(Library.timber)

    implementation(project(":core:datasource"))
}