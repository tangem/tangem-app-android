plugins {
    id("com.android.library")
    kotlin("android")
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
