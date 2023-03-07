plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.dagger.hilt.android")
    id("configuration")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(project(":domain"))
    implementation(project(":common"))
    implementation(project(":core:analytics"))
    implementation(project(":core:featuretoggles"))
    implementation(project(":core:res"))
    implementation(project(":core:ui"))
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":libs:crypto"))
    implementation(project(":libs:auth"))

    /** Features */
    implementation(project(":features:referral:presentation"))
    implementation(project(":features:referral:domain"))
    implementation(project(":features:referral:data"))
    implementation(project(":features:swap:presentation"))
    implementation(project(":features:swap:domain"))
    implementation(project(":features:swap:data"))
    implementation(project(":features:tester:api"))
    implementation(project(":features:tester:impl"))

    /** AndroidX libraries */
    implementation(AndroidX.coreKtx)
    implementation(AndroidX.appCompat)
    implementation(AndroidX.fragmentKtx)
    implementation(AndroidX.constraintLayout)
    implementation(AndroidX.browser)
    implementation(AndroidX.lifecycleRuntimeKtx)
    implementation(AndroidX.lifecycleCommonJava8)
    implementation(AndroidX.lifecycleViewModelKtx)
    implementation(AndroidX.lifecycleLiveDataKtx)
    implementation(AndroidX.activityCompose)

    /** Compose libraries */
    implementation(Compose.material)
    implementation(Compose.animation)
    implementation(Compose.foundation)
    implementation(Compose.ui)
    implementation(Compose.uiTooling)
    implementation(Compose.coil)

    /** Firebase libraries */
    implementation(platform(Firebase.bom))
    implementation(Firebase.firebaseAnalytics)
    implementation(Firebase.firebaseCrashlytics)

    /** Tangem libraries */
    implementation(Tangem.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(Tangem.cardCore)
    implementation(Tangem.cardAndroid) {
        exclude(module = "joda-time")
    }

    /** DI */
    implementation(Library.hilt)
    kapt(Library.hiltKapt)

    /** Other libraries */
    implementation(Library.materialComponent)
    implementation(Library.googlePlayCore)
    implementation(Library.googlePlayCoreKtx)
    coreLibraryDesugaring(Library.desugarJdkLibs)
    implementation(Library.timber)
    implementation(Library.reKotlin)
    implementation(Library.zxingQrCore)
    implementation(Library.zxingQrBarcodeScanner)
    implementation(Library.otaliastudiosCameraView)
    implementation(Library.coil)
    implementation(Library.appsflyer)
    implementation(Library.amplitude)
    implementation(Library.kotsonGsonExt)
// [REDACTED_TODO_COMMENT]
    implementation(Library.retrofit)
    implementation(Library.retrofitMoshiConverter)
    implementation(Library.moshi)
    implementation(Library.moshiKotlin)
    implementation(Library.okHttp)
    implementation(Library.okHttpLogging)
    implementation(Library.zendeskChat)
    implementation(Library.zendeskMessaging)
    implementation(Library.spongecastleCryptoCore)
    implementation(Library.lottie)
    implementation(Library.shopifyBuySdk) {
        exclude(group = "com.shopify.graphql.support")
        exclude(module = "joda-time")
    }
    implementation(Library.accompanistAppCompatTheme)
    implementation(Library.accompanistSystemUiController)
    implementation(Library.accompanistWebView)
    implementation(Library.xmlShimmer)
    implementation(Library.viewBindingDelegate)
    implementation(Library.armadillo)
    implementation(Library.googlePlayServicesWallet)
    implementation(Library.composeShimmer)
    implementation(Library.mviCoreWatcher)
    implementation(Library.kotlinSerialization)

    /** Testing libraries */
    testImplementation(Test.junit)
    testImplementation(Test.truth)
    androidTestImplementation(Test.junitAndroidExt)
    androidTestImplementation(Test.espresso)
}
