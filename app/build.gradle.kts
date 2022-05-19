plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    compileSdk = Config.Sdk.compileSdk

    defaultConfig {
        applicationId = Config.packageName
        minSdk = Config.Sdk.minSDK
        targetSdk = Config.Sdk.targetSDK
        versionCode = if (project.hasProperty("versionCode")) {
            project.property("versionCode") as Int
        } else {
            Config.versionCode
        }
        versionName = if (project.hasProperty("versionName")) {
            project.property("versionName") as String
        } else {
            Config.versionName
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    buildTypes {
        release {
//            signingConfig = signingConfigs.debug
            isDebuggable = false
            isMinifyEnabled = false

            BuildConfigFieldFactory(listOf(
                Field.Environment("prod"),
                Field.TestActionEnabled(false),
                Field.LogEnabled(false),
            ), ::buildConfigField).create()
        }

        debug {
//            signingConfig = signingConfigs.debug
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".dev"

            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                // disable mapping file uploads (default=true if minifying)
                mappingFileUploadEnabled = false
            }

            BuildConfigFieldFactory(listOf(
                Field.Environment("dev"),
                Field.TestActionEnabled(true),
                Field.LogEnabled(true),
            ), ::buildConfigField).create()
        }

        create("debug_beta") {
            initWith(getByName("release"))
            versionNameSuffix = "-beta"
            applicationIdSuffix = ".debug"
        }
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    compileOptions {
        sourceCompatibility = Config.Java.sourceCompatibility
        targetCompatibility = Config.Java.targetCompatibility
        isCoreLibraryDesugaringEnabled = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.0"
    }

    packagingOptions {
        excludes += "lib/x86_64/darwin/libscrypt.dylib"
        excludes += "lib/x86_64/freebsd/libscrypt.so"
        excludes += "lib/x86_64/linux/libscrypt.so"
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(project(":domain"))
    implementation(project(":network"))
    implementation(project(":common"))
    coreLibraryDesugaring(Libs.coreLibraryDesugaring)

    // AndroidX
    implementation(Libs.AndroidX.coreKtx)
    implementation(Libs.AndroidX.appcompat)
    implementation(Libs.AndroidX.fragmentKtx)
    implementation(Libs.AndroidX.constraintlayout)
    implementation(Libs.AndroidX.browser)

    // AndroidX - Lifecycle
    implementation(Libs.AndroidX.Lifecycle.commonJava8)
    implementation(Libs.AndroidX.Lifecycle.runtimeKtx)
    implementation(Libs.AndroidX.Lifecycle.viewmodelKtx)
    implementation(Libs.AndroidX.Lifecycle.livedataKtx)

    // AndroidX - Compose
    implementation(Libs.AndroidX.Compose.activityCompose)
    implementation(Libs.AndroidX.Compose.foundation)
    implementation(Libs.AndroidX.Compose.foundationLayout)
    implementation(Libs.AndroidX.Compose.material)
    implementation(Libs.AndroidX.Compose.animation)
    implementation(Libs.AndroidX.Compose.ui)
    implementation(Libs.AndroidX.Compose.uiTooling)
    implementation(Libs.AndroidX.Compose.uiToolingPreview)

    // Google
    implementation(Libs.Google.Gms.playServiceWallet)
    implementation(Libs.Google.Material.material)

    // Google - Play
    implementation(Libs.Google.Play.core)
    implementation(Libs.Google.Play.coreKtx)

    // Google - Firebase
    implementation(platform(Libs.Google.Firebase.bom))
    implementation(Libs.Google.Firebase.crashlyticsKtx)
    implementation(Libs.Google.Firebase.configKtx)
    implementation(Libs.Google.Firebase.analyticsKtx)

    // Google - Accompanist
    implementation(Libs.Google.Accompanist.theme)
    implementation(Libs.Google.Accompanist.systemUiController)

    // DI
    implementation(Libs.Di.coil)
    implementation(Libs.Di.coilCompose)

    // Camera
    implementation(Libs.Camera.zxingCore)
    implementation(Libs.Camera.barcodeScanner)
    implementation(Libs.Camera.cameraView)

    implementation(Libs.Crypto.spongycastleCore)

    implementation(Libs.picasso)
    implementation(Libs.appsFlyer)
    implementation(Libs.lottie)
    implementation(Libs.viewBinding)
    implementation(Libs.shopify) {
        exclude(
            group = "com.shopify.graphql.support",
            module = "joda-time"
        )
    }

    // Tangem sdk's
    implementation(Libs.Tangem.blockchainSdk)
    implementation(Libs.Tangem.cardCoreSdk)
    implementation(Libs.Tangem.cardAndroidSdk)

    // State management
    implementation(Libs.ReKotlin.reKotlin)

    // Kotlin coroutines
    implementation(Libs.Kotlin.coroutines)

    // Logs
    implementation(Libs.Logs.timber)

    // Network
    implementation(platform(Libs.Network.Okhttp3.bom))
    implementation(Libs.Network.Okhttp3.okhttp)
    implementation(Libs.Network.Okhttp3.loggingInterceptor)
    implementation(Libs.Network.Retrofit.retrofit)
    implementation(Libs.Network.Retrofit.converterMoshi)
    implementation(Libs.Network.JsonConverter.moshi)
    implementation(Libs.Network.JsonConverter.moshiKotlin)
    implementation(Libs.Network.JsonConverter.kotson)

    testImplementation(Libs.Test.jUnit)
    testImplementation(Libs.Test.truth)
    androidTestImplementation(Libs.Test.androidJUnit)
    androidTestImplementation(Libs.Test.espresso)
}