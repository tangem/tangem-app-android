object Libs {

    object Tangem {
        private const val cardSdkVersion = 142
        private const val blockchainSdkVersion = 77

        const val blockchainSdk = "com.tangem:blockchain:develop-$blockchainSdkVersion"
        const val cardCoreSdk = "com.tangem.tangem-sdk-kotlin:core:develop-$cardSdkVersion"
        const val cardAndroidSdk = "com.tangem.tangem-sdk-kotlin:android:develop-$cardSdkVersion"
    }

    object Kotlin {
        const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2"
    }

    object ReKotlin {
        const val reKotlin = "org.rekotlin:rekotlin:1.0.4"
    }

    object Google {
        object Material {
            const val material = "com.google.android.material:material:1.5.0"
        }

        object Play {
            const val core = "com.google.android.play:core:1.10.3"
            const val coreKtx = "com.google.android.play:core-ktx:1.8.1"
        }

        object Gms {
            const val playServiceWallet = "com.google.android.gms:play-services-wallet:19.1.0"
        }

        object Firebase {
            const val bom = "com.google.firebase:firebase-bom:26.0.0"
            const val crashlyticsKtx = "com.google.firebase:firebase-crashlytics-ktx"
            const val configKtx = "com.google.firebase:firebase-config-ktx"
            const val analyticsKtx = "com.google.firebase:firebase-analytics-ktx"
        }

        object Accompanist {
            // it based on Compose.version - https://github.com/google/accompanist#compose-versions
            private const val version = "0.23.1"
            const val theme = "com.google.accompanist:accompanist-appcompat-theme:$version"
            const val systemUiController = "com.google.accompanist:accompanist-systemuicontroller:$version"
        }
    }

    object AndroidX {
        const val coreKtx = "androidx.core:core-ktx:1.7.0"
        const val appcompat = "androidx.appcompat:appcompat:1.4.1"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:1.4.1"
        const val constraintlayout = "androidx.constraintlayout:constraintlayout:2.1.3"
        const val browser = "androidx.browser:browser:1.3.0"

        object Lifecycle {
            private const val version = "2.3.1"
            const val runtimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
            const val commonJava8 = "androidx.lifecycle:lifecycle-common-java8:$version"
            const val viewmodelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
            const val livedataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
        }

        object Compose {
            const val activityCompose = "androidx.activity:activity-compose:1.4.0"

            private const val version = "1.1.1"
            const val foundation = "androidx.compose.foundation:foundation:$version"
            const val foundationLayout = "androidx.compose.foundation:foundation-layout:$version"
            const val material = "androidx.compose.material:material:$version"
            const val animation = "androidx.compose.animation:animation:$version"
            const val ui = "androidx.compose.ui:ui:$version"
            const val uiTooling = "androidx.compose.ui:ui-tooling:$version"
            const val uiToolingPreview = "androidx.compose.ui:ui-tooling-preview:$version"
//            const val runtimeLivedata = "androidx.compose.runtime:runtime-livedata:$version"
//            const val uiTest = "androidx.compose.ui:ui-test-junit4:$version"
        }
    }

    object Di {
        private const val version = "2.0.0-rc01"
        const val coil = "io.coil-kt:coil:$version"
        const val coilCompose = "io.coil-kt:coil-compose:$version"
    }

    object Network {
        object Okhttp3 {
            const val bom = "com.squareup.okhttp3:okhttp-bom:4.9.3"
            const val okhttp = "com.squareup.okhttp3:okhttp"
            const val loggingInterceptor = "com.squareup.okhttp3:logging-interceptor"
        }

        object Retrofit {
            const val retrofit = "com.squareup.retrofit2:retrofit:2.8.1"
            const val converterMoshi = "com.squareup.retrofit2:converter-moshi:2.6.0"

        }

        object JsonConverter {
            private const val moshiVersion = "1.13.0"
            const val moshi = "com.squareup.moshi:moshi:$moshiVersion"
            const val moshiKotlin = "com.squareup.moshi:moshi-kotlin:$moshiVersion"

            const val kotson = "com.github.salomonbrys.kotson:kotson:2.5.0"

        }
    }

    object Crypto {
        const val spongycastleCore = "com.madgag.spongycastle:core:1.58.0.0"
    }

    object Camera {
        const val zxingCore = "com.google.zxing:core:3.3.3"
        const val barcodeScanner = "me.dm7.barcodescanner:zxing:1.9.8"
        const val cameraView = "com.otaliastudios:cameraview:2.6.3"
    }


    object Logs {
        const val timber = "com.jakewharton.timber:timber:4.7.1"
    }

    object Test {
        const val jUnit = "junit:junit:4.13.2"
        const val truth = "com.google.truth:truth:1.1.3"
        const val androidJUnit = "androidx.test.ext:junit:1.1.3"
        const val espresso = "androidx.test.espresso:espresso-core:3.4.0"
    }

    const val coreLibraryDesugaring = "com.android.tools:desugar_jdk_libs:1.1.5"
    const val viewBinding = "com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.5.6"
    const val shopify = "com.shopify.mobilebuysdk:buy3:12.0.0"
    const val appsFlyer = "com.appsflyer:af-android-sdk:6.5.1"
    const val picasso = "com.squareup.picasso:picasso:2.71828"
    const val lottie = "com.airbnb.android:lottie:3.4.0"

}