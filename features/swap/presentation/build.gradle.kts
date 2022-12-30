plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
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

    buildTypes {
        create("debug_beta") {
            initWith(getByName("release"))
            BuildConfigFieldFactory(
                fields = listOf(
                    Field.Environment("release"),
                    Field.TestActionEnabled(true),
                    Field.LogEnabled(true),
                ),
                builder = ::buildConfigField,
            ).create()
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.compose
    }
}

dependencies {
    /** Core modules */
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))

    /** AndroidX */
    implementation(AndroidX.appCompat)
    implementation(AndroidX.fragmentKtx)
    implementation(AndroidX.lifecycleViewModelKtx)

    /** Compose */
    implementation(Compose.foundation)
    implementation(Compose.material)
    implementation(Compose.uiTooling)
    implementation(Compose.coil)

    /** Domain */
    implementation(project(":features:swap:domain"))
    implementation(project(":features:swap:data"))//todo remove, only for test

    /** Other libraries */
    implementation(Library.composeShimmer)
    implementation(Library.kotlinSerialization)

    /** DI */
    implementation(Library.hilt)
    kapt(Library.hiltKapt)
}
