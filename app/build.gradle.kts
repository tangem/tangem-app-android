plugins {
    alias(deps.plugins.android.application)
    alias(deps.plugins.kotlin.android)
    alias(deps.plugins.kotlin.kapt)
    alias(deps.plugins.kotlin.serialization)
    alias(deps.plugins.google.services)
    alias(deps.plugins.hilt.android)
    alias(deps.plugins.firebase.crashlytics)
    id("configuration")
}

configurations.all {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
    exclude(group = "com.github.komputing.kethereum")

    resolutionStrategy {
        dependencySubstitution {
            substitute(module("com.facebook.react:react-native"))
                .using(module("com.facebook.react:react-android:0.72.4"))

            substitute(module("com.facebook.react:hermes-engine"))
                .using(module("com.facebook.react:hermes-android:0.72.4"))

            substitute(module("org.bouncycastle:bcprov-jdk15on"))
                .using(module("org.bouncycastle:bcprov-jdk18on:1.73"))
        }

        force(
            "org.bouncycastle:bcpkix-jdk15on:1.70",
            "com.facebook.react:react-android:0.72.4",
            "com.facebook.react:hermes-android:0.72.4",
        )
    }
}


dependencies {
    implementation(files("libs/walletconnect-1.5.6.aar"))
    implementation(projects.domain.legacy)
    implementation(projects.domain.models)
    implementation(projects.domain.core)
    implementation(projects.domain.card)
    implementation(projects.domain.demo)
    implementation(projects.domain.wallets)
    implementation(projects.domain.wallets.models)
    implementation(projects.domain.settings)
    implementation(projects.domain.tokens)
    implementation(projects.domain.tokens.models)
    implementation(projects.domain.txhistory)
    implementation(projects.domain.appCurrency)
    implementation(projects.domain.appCurrency.models)
    implementation(projects.domain.appTheme)
    implementation(projects.domain.appTheme.models)
    implementation(projects.domain.balanceHiding)
    implementation(projects.domain.balanceHiding.models)
    implementation(projects.domain.transaction)
    implementation(projects.domain.analytics)
    implementation(projects.domain.visa)

    implementation(projects.common)
    implementation(projects.core.analytics)
    implementation(projects.core.analytics.models)
    implementation(projects.core.navigation)
    implementation(projects.core.featuretoggles)
    implementation(projects.core.res)
    implementation(projects.core.ui)
    implementation(projects.core.datasource)
    implementation(projects.core.utils)
    implementation(projects.core.deepLinks)
    implementation(projects.libs.crypto)
    implementation(projects.libs.auth)

    implementation(projects.data.appCurrency)
    implementation(projects.data.appTheme)
    implementation(projects.data.balanceHiding)
    implementation(projects.data.card)
    implementation(projects.data.common)
    implementation(projects.data.settings)
    implementation(projects.data.source.preferences)
    implementation(projects.data.tokens)
    implementation(projects.data.txhistory)
    implementation(projects.data.wallets)
    implementation(projects.data.analytics)
    implementation(projects.data.transaction)
    implementation(projects.data.visa)

    /** Features */
    implementation(projects.features.onboarding)
    implementation(projects.features.referral.presentation)
    implementation(projects.features.referral.domain)
    implementation(projects.features.referral.data)
    implementation(projects.features.swap.api)
    implementation(projects.features.swap.presentation)
    implementation(projects.features.swap.domain)
    implementation(projects.features.swap.domain.api)
    implementation(projects.features.swap.data)
    implementation(projects.features.tester.api)
    implementation(projects.features.tester.impl)
    implementation(projects.features.wallet.api)
    implementation(projects.features.wallet.impl)
    implementation(projects.features.tokendetails.api)
    implementation(projects.features.tokendetails.impl)
    implementation(projects.features.send.api)
    implementation(projects.features.manageTokens.api)
    implementation(projects.features.manageTokens.impl)
    implementation(projects.features.send.impl)
    implementation(projects.features.qrScanning.api)
    implementation(projects.features.qrScanning.impl)

    /** AndroidX libraries */
    implementation(deps.androidx.core.ktx)
    implementation(deps.androidx.core.splashScreen)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.constraintLayout)
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.browser)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.androidx.swipeRefreshLayout)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(deps.lifecycle.common.java8)
    implementation(deps.lifecycle.viewModel.ktx)
    implementation(deps.lifecycle.compose)

    /** Compose libraries */
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.material)
    implementation(deps.compose.material3)
    implementation(deps.compose.animation)
    implementation(deps.compose.coil)
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.foundation)
    implementation(deps.compose.material)
    implementation(deps.compose.navigation.hilt)
    implementation(deps.compose.shimmer)
    implementation(deps.compose.ui)
    implementation(deps.compose.ui.tooling)
    implementation(deps.compose.paging)

    /** Firebase libraries */
    implementation(platform(deps.firebase.bom))
    implementation(deps.firebase.analytics)
    implementation(deps.firebase.crashlytics)

    /** Tangem libraries */
    implementation(deps.tangem.blockchain) {
        exclude(module = "joda-time")
    }
    implementation(deps.tangem.card.core)
    implementation(deps.tangem.card.android) {
        exclude(module = "joda-time")
    }

    /** DI */
    implementation(deps.hilt.android)

    kapt(deps.hilt.kapt)

    /** Other libraries */
    implementation(deps.kotlin.immutable.collections)
    implementation(deps.material)
    implementation(deps.googlePlay.core)
    implementation(deps.googlePlay.core.ktx)
    implementation(deps.googlePlay.services.wallet)
    coreLibraryDesugaring(deps.desugar)
    implementation(deps.timber)
    implementation(deps.reKotlin)
    implementation(deps.zxing.qrCore)
    implementation(deps.coil)
    implementation(deps.appsflyer)
    implementation(deps.amplitude)
    implementation(deps.kotsonGson)
    implementation(deps.spongecastle.core)
    implementation(deps.lottie)
    implementation(deps.shopify.buy) {
        exclude(group = "com.shopify.graphql.support")
        exclude(module = "joda-time")
    }
    implementation(deps.compose.accompanist.appCompatTheme)
    implementation(deps.compose.accompanist.systemUiController)
    implementation(deps.compose.accompanist.webView)
    implementation(deps.xmlShimmer)
    implementation(deps.viewBindingDelegate)
    implementation(deps.armadillo)
    implementation(deps.mviCore.watcher)
    implementation(deps.kotlin.serialization)
    implementation(deps.walletConnectCore)
    implementation(deps.walletConnectWeb3)
    implementation(deps.prettyLogger)
    implementation("com.facebook.react:react-android:0.72.4")
    implementation(deps.sprClient) {
        exclude(group = "com.github.stephenc.jcip")
    }

    /** Testing libraries */
    testImplementation(deps.test.coroutine)
    testImplementation(deps.test.junit)
    testImplementation(deps.test.mockk)
    testImplementation(deps.test.truth)
    androidTestImplementation(deps.test.junit.android)
    androidTestImplementation(deps.test.espresso)

    /** Chucker */
    debugImplementation(deps.chucker)
    externalImplementation(deps.chuckerStub)
    internalImplementation(deps.chuckerStub)
    releaseImplementation(deps.chuckerStub)

    /** Camera */
    implementation(deps.camera.camera2)
    implementation(deps.camera.lifecycle)
    implementation(deps.camera.view)

    implementation(deps.listenableFuture)
    implementation(deps.mlKit.barcodeScanning)

    /** Excluded dependencies */
    implementation("com.google.guava:guava:30.0-android") {
        // excludes version 9999.0-empty-to-avoid-conflict-with-guava
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

}
