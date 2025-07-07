package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.feature.referral.api.deeplink.ReferralDeepLinkHandler
import com.tangem.features.markets.deeplink.MarketsDeepLinkHandler
import com.tangem.features.markets.deeplink.MarketsTokenDetailDeepLinkHandler
import com.tangem.features.onramp.deeplink.BuyDeepLinkHandler
import com.tangem.features.onramp.deeplink.OnrampDeepLinkHandler
import com.tangem.features.onramp.deeplink.SellDeepLinkHandler
import com.tangem.features.onramp.deeplink.SwapDeepLinkHandler
import com.tangem.features.send.v2.api.deeplink.SellRedirectDeepLinkHandler
import com.tangem.features.staking.api.deeplink.StakingDeepLinkHandler
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class DeepLinkFactoryTest {

    private val onrampDeepLinkFactory = mockk<OnrampDeepLinkHandler.Factory>(relaxed = true) {
        every { create(any(), any()) } returns mockk()
    }
    private val sellRedirectDeepLinkFactory = mockk<SellRedirectDeepLinkHandler.Factory>(relaxed = true) {
        every { create(any(), any()) } returns mockk()
    }
    private val referralDeepLinkFactory = mockk<ReferralDeepLinkHandler.Factory>(relaxed = true) {
        every { create() } returns mockk()
    }
    private val walletConnectDeepLinkFactory = mockk<WalletConnectDeepLinkHandler.Factory>(relaxed = true) {
        every { create(any()) } returns mockk()
    }
    private val walletDeepLinkFactory = mockk<WalletDeepLinkHandler.Factory>(relaxed = true) {
        every { create() } returns mockk()
    }
    private val tokenDetailsDeepLinkFactory = mockk<TokenDetailsDeepLinkHandler.Factory>(relaxed = true) {
        every { create(any(), any(), any()) } returns mockk()
    }
    private val stakingDeepLinkFactory = mockk<StakingDeepLinkHandler.Factory>(relaxed = true) {
        every { create(any(), any()) } returns mockk()
    }
    private val marketsDeepLinkFactory = mockk<MarketsDeepLinkHandler.Factory>(relaxed = true) {
        every { create() } returns mockk()
    }
    private val marketsTokenDetailDeepLinkFactory = mockk<MarketsTokenDetailDeepLinkHandler.Factory>(relaxed = true) {
        every { create(any(), any()) } returns mockk()
    }
    private val sellDeepLinkFactory = mockk<SellDeepLinkHandler.Factory>(relaxed = true) {
        every { create() } returns mockk()
    }
    private val buyDeepLinkFactory = mockk<BuyDeepLinkHandler.Factory>(relaxed = true) {
        every { create() } returns mockk()
    }
    private val swapDeepLinkFactory = mockk<SwapDeepLinkHandler.Factory>(relaxed = true) {
        every { create() } returns mockk()
    }

    private val cardSdkProvider = mockk<CardSdkProvider>(relaxed = true) {
        every { sdk.uiVisibility() } returns MutableStateFlow(false)
    }
    private val mockedUri = mockk<Uri>(relaxed = true)
    private val isFromOnNewIntent: Boolean = false

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    private val deepLinkFactory = DeepLinkFactory(
        cardSdkProvider = cardSdkProvider,
        onrampDeepLink = onrampDeepLinkFactory,
        sellRedirectDeepLink = sellRedirectDeepLinkFactory,
        referralDeepLink = referralDeepLinkFactory,
        walletConnectDeepLink = walletConnectDeepLinkFactory,
        walletDeepLink = walletDeepLinkFactory,
        tokenDetailsDeepLink = tokenDetailsDeepLinkFactory,
        stakingDeepLink = stakingDeepLinkFactory,
        marketsDeepLink = marketsDeepLinkFactory,
        marketsTokenDetailDeepLink = marketsTokenDetailDeepLinkFactory,
        buyDeepLink = buyDeepLinkFactory,
        sellDeepLink = sellDeepLinkFactory,
        swapDeepLink = swapDeepLinkFactory,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)

        Dispatchers.setMain(testDispatcher)

        every { mockedUri.path } returns "/path"
        every { mockedUri.toString() } returns "https://example.com/path?query=param"
        every { mockedUri.authority } returns "example.com"
        every { mockedUri.port } returns 443 // Default HTTPS port
        every { mockedUri.fragment } returns null // No fragment in this URI

        Timber.uprootAll() // Disable Timber logging for tests
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        // Reset the main dispatcher
        Dispatchers.resetMain()
        // Clean up test coroutines
        testScope.cancel()
    }

    @Test
    fun `handleDeeplink stores uri and launches when permitted`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"
        every { mockedUri.query } returns "param=value"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        // Set permittedAppRoute to true
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)

        advanceUntilIdle()

        // Verify onramp handler was called
        verify {
            onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value")))
        }
    }

    @Test
    fun `handleDeeplink does not launch when not permitted`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"
        every { mockedUri.query } returns "param=value"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Initial)

        advanceUntilIdle()

        // Verify no handler was called
        verify(inverse = true) { onrampDeepLinkFactory.create(any(), any()) }
    }

    @Test
    fun `handleDeeplink does not launch when card scan visible`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"
        every { mockedUri.query } returns "param=value"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"
        every { cardSdkProvider.sdk.uiVisibility() } returns MutableStateFlow(true)

        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)

        advanceUntilIdle()

        // Verify no handler was called
        verify(inverse = true) { onrampDeepLinkFactory.create(any(), any()) }
    }

    @Test
    fun `launchDeepLink handles tangem scheme correctly`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"
        every { mockedUri.query } returns "param=value"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)

        advanceUntilIdle()

        verify {
            onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value")))
        }
    }

    @Test
    fun `launchDeepLink handles wc scheme correctly`() = runTest {
        every { mockedUri.scheme } returns "wc"
        every { mockedUri.host } returns ""

        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)

        advanceUntilIdle()

        verify {
            walletConnectDeepLinkFactory.create(eq(mockedUri))
        }
    }

    @Test
    fun `launchDeepLink ignores unknown scheme`() = runTest {
        every { mockedUri.scheme } returns "https"
        every { mockedUri.host } returns "example.com"

        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)

        advanceUntilIdle()

        verify(inverse = true) {
            onrampDeepLinkFactory.create(any(), any())
            sellRedirectDeepLinkFactory.create(any(), any())
            referralDeepLinkFactory.create()
            walletConnectDeepLinkFactory.create(any())
            walletDeepLinkFactory.create()
            tokenDetailsDeepLinkFactory.create(any(), any(), any())
        }
    }

    @Test
    fun `handleTangemDeepLinks routes to correct handler`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.query } returns "param=value"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)

        // Test Onramp
        every { mockedUri.host } returns "onramp"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value"))) }

        // Test Sell Redirect
        every { mockedUri.host } returns "redirect_sell"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { sellRedirectDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value"))) }

        // Test Token Details
        every { mockedUri.host } returns "token"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify {
            tokenDetailsDeepLinkFactory.create(
                eq(testScope),
                eq(mapOf("param" to "value")),
                eq(isFromOnNewIntent),
            )
        }

        // Test Staking
        every { mockedUri.host } returns "staking"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { stakingDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value"))) }

        // Test Market Token Detail
        every { mockedUri.host } returns "token_chart"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { marketsTokenDetailDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value"))) }

        // Reset params
        every { mockedUri.queryParameterNames } returns emptySet()
        every { mockedUri.getQueryParameter(any()) } returns ""

        // Test Referral
        every { mockedUri.host } returns "referral"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { referralDeepLinkFactory.create() }

        // Test Wallet
        every { mockedUri.host } returns "main"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { walletDeepLinkFactory.create() }

        // Test Markets
        every { mockedUri.host } returns "markets"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { marketsDeepLinkFactory.create() }

        // Test Sell
        every { mockedUri.host } returns "sell"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { sellDeepLinkFactory.create() }

        // Test Swap
        every { mockedUri.host } returns "swap"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { sellDeepLinkFactory.create() }

        // Test Buy
        every { mockedUri.host } returns "buy"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { buyDeepLinkFactory.create() }
    }

    @Test
    fun `handleTangemDeepLinks incorrect host`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "unknown"

        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)

        advanceUntilIdle()

        verify(inverse = true) {
            onrampDeepLinkFactory.create(any(), any())
            sellRedirectDeepLinkFactory.create(any(), any())
            referralDeepLinkFactory.create()
            walletConnectDeepLinkFactory.create(any())
            walletDeepLinkFactory.create()
            tokenDetailsDeepLinkFactory.create(any(), any(), any())
            buyDeepLinkFactory.create()
            sellDeepLinkFactory.create()
            swapDeepLinkFactory.create()
        }
    }

    @Test
    fun `getParams filters malicious parameters`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"
        every { mockedUri.query } returns "safe=ok&malicious=%3Cscript%3E"
        every { mockedUri.queryParameterNames } returns setOf("safe", "malicious")
        every { mockedUri.getQueryParameter("safe") } returns "ok"
        every { mockedUri.getQueryParameter("malicious") } returns "<script>"

        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()

        verify { onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("safe" to "ok"))) }
    }

    @Test
    fun `validate detects malicious characters`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"

        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)

        every { mockedUri.query } returns "safe=ok"
        every { mockedUri.queryParameterNames } returns setOf("safe")
        every { mockedUri.getQueryParameter("safe") } returns "ok"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("safe" to "ok"))) }

        every { mockedUri.query } returns "param123=ok"
        every { mockedUri.queryParameterNames } returns setOf("param123")
        every { mockedUri.getQueryParameter("param123") } returns "ok"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("param123" to "ok"))) }

        every { mockedUri.query } returns "unsafe=<script>"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "<script>"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }

        every { mockedUri.query } returns "unsafe=test;"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "test;"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }

        every { mockedUri.query } returns "unsafe=test+attack"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "test+attack"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }

        every { mockedUri.query } returns "unsafe=test\\path"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "test\\path"
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }
    }
}