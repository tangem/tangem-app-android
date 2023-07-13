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

//conflict of dependencies when adding WalletConnectV2.0 library
configurations {
    all {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
        resolutionStrategy {
            force("org.bouncycastle:bcpkix-jdk15on:1.70")
        }
        exclude(group = "com.github.komputing.kethereum")
    }
}

dependencies {
    implementation(files("libs/walletconnect-1.5.6.aar"))
    implementation(project(":domain:legacy"))
    implementation(project(":domain:models"))
    implementation(project(":domain:core"))
    implementation(projects.domain.card)
    implementation(project(":domain:wallets"))
    implementation(project(":domain:wallets:models"))
    implementation(projects.domain.tokens)
    implementation(project(":common"))
    implementation(project(":core:analytics"))
    implementation(projects.core.analytics.models)
    implementation(project(":core:navigation"))
    implementation(project(":core:featuretoggles"))
    implementation(project(":core:res"))
    implementation(project(":core:ui"))
    implementation(project(":core:datasource"))
    implementation(project(":core:utils"))
    implementation(project(":libs:crypto"))
    implementation(project(":libs:auth"))
    implementation(project(":data:source:preferences"))
    implementation(projects.data.card)
    implementation(projects.data.tokens)

    /** Features */
    implementation(project(":features:onboarding"))
    implementation(project(":features:learn2earn:api"))
    implementation(project(":features:learn2earn:impl"))
    implementation(project(":features:referral:presentation"))
    implementation(project(":features:referral:domain"))
    implementation(project(":features:referral:data"))
    implementation(project(":features:swap:api"))
    implementation(project(":features:swap:presentation"))
    implementation(project(":features:swap:domain"))
    implementation(project(":features:swap:data"))
    implementation(project(":features:tester:api"))
    implementation(project(":features:tester:impl"))
    implementation(project(":features:wallet:api"))
    implementation(project(":features:wallet:impl"))
    implementation(projects.features.tokendetails.api)
    implementation(projects.features.tokendetails.impl)

    /** AndroidX libraries */
    implementation(deps.androidx.core.ktx)
    implementation(deps.androidx.core.splashScreen)
    implementation(deps.androidx.appCompat)
    implementation(deps.androidx.fragment.ktx)
    implementation(deps.androidx.constraintLayout)
    implementation(deps.androidx.activity.compose)
    implementation(deps.androidx.browser)
    implementation(deps.androidx.paging.runtime)
    implementation(deps.lifecycle.runtime.ktx)
    implementation(deps.lifecycle.common.java8)
    implementation(deps.lifecycle.viewModel.ktx)

    /** Compose libraries */
    implementation(deps.compose.constraintLayout)
    implementation(deps.compose.material)
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
    implementation(deps.zxing.qrBarcodeScanner)
    implementation(deps.otaliastudiosCameraView)
    implementation(deps.coil)
    implementation(deps.appsflyer)
    implementation(deps.amplitude)
    implementation(deps.kotsonGson)
    implementation(deps.zendesk.chat)
    implementation(deps.zendesk.messaging)
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

    /** Testing libraries */
    testImplementation(deps.test.junit)
    testImplementation(deps.test.truth)
    androidTestImplementation(deps.test.junit.android)
    androidTestImplementation(deps.test.espresso)
}
