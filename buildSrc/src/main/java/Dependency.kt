object AndroidX {
    const val activityCompose = "androidx.activity:activity-compose:" + Versions.androidxActivityCompose
    const val appCompat = "androidx.appcompat:appcompat:" + Versions.androidxAppCompat
    const val browser = "androidx.browser:browser:" + Versions.androidxBrowser
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:" + Versions.androidxConstraintLayout
    const val coreKtx = "androidx.core:core-ktx:" + Versions.androidxCore
    const val fragmentKtx = "androidx.fragment:fragment-ktx:" + Versions.androidxFragment
    const val lifecycleCommonJava8 = "androidx.lifecycle:lifecycle-common-java8:" + Versions.androidxLifecycle
    const val lifecycleLiveDataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:" + Versions.androidxLifecycle
    const val lifecycleRuntimeKtx = "androidx.lifecycle:lifecycle-runtime-ktx:" + Versions.androidxLifecycle
    const val lifecycleViewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:" + Versions.androidxLifecycle
}

object Compose {
    const val animation = "androidx.compose.animation:animation:" + Versions.compose
    const val coil = "io.coil-kt:coil-compose:" + Versions.coil
    const val foundation = "androidx.compose.foundation:foundation:" + Versions.composeFoundation
    const val material = "androidx.compose.material:material:" + Versions.compose
    const val ui = "androidx.compose.ui:ui:" + Versions.compose
    const val uiTooling = "androidx.compose.ui:ui-tooling:" + Versions.compose
}

object Firebase {
    const val bom = "com.google.firebase:firebase-bom:" + Versions.firebase
    const val firebaseAnalytics = "com.google.firebase:firebase-analytics-ktx"
    const val firebaseCrashlytics = "com.google.firebase:firebase-crashlytics-ktx"
}

object Library {
    const val accompanistAppCompatTheme = "com.google.accompanist:accompanist-appcompat-theme:" + Versions.accompanist
    const val accompanistSystemUiController =
        "com.google.accompanist:accompanist-systemuicontroller:" + Versions.accompanist
    const val amplitude = "com.amplitude:android-sdk:" + Versions.amplitude
    const val appsflyer = "com.appsflyer:af-android-sdk:" + Versions.appsflyer
    const val armadillo = "at.favre.lib:armadillo:" + Versions.armadillo
    const val coil = "io.coil-kt:coil:" + Versions.coil
    const val composeShimmer = "com.valentinilk.shimmer:compose-shimmer:" + Versions.composeShimmer
    const val coroutine = "org.jetbrains.kotlinx:kotlinx-coroutines-core:" + Versions.coroutine
    const val desugarJdkLibs = "com.android.tools:desugar_jdk_libs:" + Versions.desugarJdkLibs
    const val googlePlayCore = "com.google.android.play:core:" + Versions.googlePlayCore
    const val googlePlayCoreKtx = "com.google.android.play:core-ktx:" + Versions.googlePlayCoreKtx
    const val googlePlayServicesWallet =
        "com.google.android.gms:play-services-wallet:" + Versions.googlePlayServicesWallet
    const val hilt = "com.google.dagger:hilt-android:" + Versions.hilt
    const val hiltCore = "com.google.dagger:hilt-core:" + Versions.hilt
    const val hiltKapt = "com.google.dagger:hilt-compiler:" + Versions.hilt
    const val jodatime = "joda-time:joda-time:" + Versions.jodatime
    const val kotsonGsonExt = "com.github.salomonbrys.kotson:kotson:" + Versions.kotsonGsonExt
    const val lottie = "com.airbnb.android:lottie:" + Versions.lottie
    const val materialComponent = "com.google.android.material:material:" + Versions.googleMaterialComponent
    const val moshi = "com.squareup.moshi:moshi:" + Versions.moshi
    const val moshiKotlin = "com.squareup.moshi:moshi-kotlin:" + Versions.moshi
    const val okHttp = "com.squareup.okhttp3:okhttp:" + Versions.okhttp
    const val okHttpLogging = "com.squareup.okhttp3:logging-interceptor:" + Versions.okhttp
    const val otaliastudiosCameraView = "com.otaliastudios:cameraview:" + Versions.otaliastudiosCameraView
    const val shopifyBuySdk = "com.shopify.mobilebuysdk:buy3:" + Versions.shopifyBuySdk
    const val spongecastleCryptoCore = "com.madgag.spongycastle:core:" + Versions.spongycastleCryptoCore
    const val reKotlin = "org.rekotlin:rekotlin:" + Versions.rekotlin
    const val retrofit = "com.squareup.retrofit2:retrofit:" + Versions.retrofit
    const val retrofitMoshiConverter = "com.squareup.retrofit2:converter-moshi:" + Versions.retrofitMoshiConverter
    const val timber = "com.jakewharton.timber:timber:" + Versions.timber
    const val viewBindingDelegate =
        "com.github.kirich1409:viewbindingpropertydelegate-noreflection:" + Versions.viewBindingDelegate
    const val xmlShimmer = "com.github.skydoves:androidveil:" + Versions.xmlShimmer
    const val zendeskChat = "com.zendesk:chat:" + Versions.zendeskChat
    const val zendeskMessaging = "com.zendesk:messaging:" + Versions.zendeskMessaging
    const val zxingQrBarcodeScanner = "me.dm7.barcodescanner:zxing:" + Versions.zxingQrBarcodeScanner
    const val zxingQrCore = "com.google.zxing:core:" + Versions.zxingQrCode
    const val mviCoreWatcher = "com.github.badoo.mvicore:mvicore-diff:" + Versions.mviCore
    const val kotlinSerialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:" + Versions.kotlinSerialization
}

object Tangem {
    const val blockchain = "com.tangem:blockchain:" + Versions.tangemBlockchainSdk
    const val cardAndroid = "com.tangem.tangem-sdk-kotlin:android:" + Versions.tangemCardSdk
    const val cardCore = "com.tangem.tangem-sdk-kotlin:core:" + Versions.tangemCardSdk
}

object Tools {
    const val composeDetektRules = "ru.kode:detekt-rules-compose:" + Versions.composeDetektRules
    const val formattingDetektRules = "io.gitlab.arturbosch.detekt:detekt-formatting:" + Versions.formattingDetektRules
}

object Test {
    const val espresso = "androidx.test.espresso:espresso-core:" + Versions.espresso
    const val junit = "junit:junit:" + Versions.junit
    const val junitAndroidExt = "androidx.test.ext:junit:" + Versions.junitAndroidExt
    const val truth = "com.google.truth:truth:" + Versions.truth
}
