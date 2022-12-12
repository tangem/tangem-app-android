plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

android {

    defaultConfig {
        compileSdk = AppConfig.compileSdkVersion
        minSdk = AppConfig.minSdkVersion
        targetSdk = AppConfig.targetSdkVersion
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = false
    }
}

dependencies {

    /** Project*/
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":features:swap:domain"))

    /** DI */
    implementation(Library.hilt)
    kapt(Library.hiltKapt)
}