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
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        release {
            BuildConfigFieldFactory(
                fields = listOf(Field.TesterMenuAvailability(false)),
                builder = ::buildConfigField,
            ).create()
        }

        debug {
            BuildConfigFieldFactory(
                fields = listOf(Field.TesterMenuAvailability(true)),
                builder = ::buildConfigField,
            ).create()
        }

        create("debug_beta") {
            initWith(getByName("release"))
            BuildConfigFieldFactory(
                fields = listOf(
                    Field.Environment("release"),
                    Field.TestActionEnabled(true),
                    Field.LogEnabled(true),
                    Field.TesterMenuAvailability(true),
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
