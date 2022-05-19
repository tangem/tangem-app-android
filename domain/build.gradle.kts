plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = Config.Sdk.compileSdk

    defaultConfig {
        minSdk = Config.Sdk.minSDK
        targetSdk = Config.Sdk.targetSDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            BuildConfigFieldFactory(listOf(
                Field.Environment("prod"),
                Field.TestActionEnabled(false),
                Field.LogEnabled(false),
            ), ::buildConfigField).create()
        }

        debug {
            isMinifyEnabled = false
            BuildConfigFieldFactory(listOf(
                Field.Environment("prod"),
                Field.TestActionEnabled(true),
                Field.LogEnabled(true),
            ), ::buildConfigField).create()
        }

        create("debug_beta") {
            initWith(getByName("release"))
            BuildConfigFieldFactory(listOf(
                Field.LogEnabled(true),
            ), ::buildConfigField).create()
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

    packagingOptions {
        excludes += "lib/x86_64/darwin/libscrypt.dylib"
        excludes += "lib/x86_64/freebsd/libscrypt.so"
        excludes += "lib/x86_64/linux/libscrypt.so"
    }
}

dependencies {
    implementation(project(":network"))
    implementation(project(":common"))

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

    // Tests
    testImplementation(Libs.Test.jUnit)
    testImplementation(Libs.Test.truth)
    androidTestImplementation(Libs.Test.androidJUnit)
    androidTestImplementation(Libs.Test.espresso)
}