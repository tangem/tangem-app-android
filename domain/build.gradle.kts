plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = AppConfig.compileSdkVersion

    defaultConfig {
        minSdk = AppConfig.minSdkVersion
        targetSdk = AppConfig.targetSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

            BuildConfigFieldFactory(
                fields = listOf(
                    Field.Environment("prod"),
                    Field.TestActionEnabled(false),
                    Field.LogEnabled(false),
                ),
                builder = ::buildConfigField,
            ).create()
        }

        debug {
            isMinifyEnabled = false

            BuildConfigFieldFactory(
                fields = listOf(
                    Field.Environment("prod"),
                    Field.TestActionEnabled(true),
                    Field.LogEnabled(true),
                ),
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
                ),
                builder = ::buildConfigField,
            ).create()
        }
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packagingOptions {
        resources.excludes += "lib/x86_64/darwin/libscrypt.dylib"
        resources.excludes += "lib/x86_64/freebsd/libscrypt.so"
        resources.excludes += "lib/x86_64/linux/libscrypt.so"
    }
}

dependencies {
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":common"))
    implementation(project(":libs:auth"))

    /** Tangem libraries */
    implementation(Tangem.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(Tangem.cardCore)
    implementation(Tangem.cardAndroid) {
        exclude(module = "joda-time")
    }

    /** Other libraries */
    implementation(Library.reKotlin)
    //TODO: refactoring: remove it when all network services moved to the datasource module
    implementation(Library.retrofit)
    implementation(Library.retrofitMoshiConverter)
    implementation(Library.moshi)
    implementation(Library.moshiKotlin)
    implementation(Library.okHttpLogging)
    implementation(Library.timber)
    implementation(Library.coroutine)

    /** Testing libraries */
    testImplementation(Test.junit)
    testImplementation(Test.truth)
    androidTestImplementation(Test.junitAndroidExt)
    androidTestImplementation(Test.espresso)
}
