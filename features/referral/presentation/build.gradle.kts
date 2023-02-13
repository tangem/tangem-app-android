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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.compose
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
