package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.feature.referral.api.deeplink.ReferralDeepLinkHandler
import com.tangem.features.onramp.deeplink.BuyDeepLinkHandler
import com.tangem.features.onramp.deeplink.OnrampDeepLinkHandler
import com.tangem.features.send.v2.api.deeplink.SellDeepLinkHandler
import com.tangem.features.tokendetails.deeplink.TokenDetailsDeepLinkHandler
import com.tangem.features.wallet.deeplink.WalletDeepLinkHandler
import com.tangem.features.walletconnect.components.deeplink.WalletConnectDeepLinkHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
    private val sellDeepLinkFactory = mockk<SellDeepLinkHandler.Factory>(relaxed = true) {
        every { create(any(), any()) } returns mockk()
    }
    private val buyDeepLinkFactory = mockk<BuyDeepLinkHandler.Factory>(relaxed = true) {
        every { create(any()) } returns mockk()
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
        every { create(any(), any()) } returns mockk()
    }

    private val mockedUri = mockk<Uri>(relaxed = true)

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    private val deepLinkFactory = DeepLinkFactory(
        onrampDeepLinkFactory,
        sellDeepLinkFactory,
        buyDeepLinkFactory,
        referralDeepLinkFactory,
        walletConnectDeepLinkFactory,
        walletDeepLinkFactory,
        tokenDetailsDeepLinkFactory,
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
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
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

        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Initial)

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
        deepLinkFactory.handleDeeplink(mockedUri, testScope)

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
        deepLinkFactory.handleDeeplink(mockedUri, testScope)

        advanceUntilIdle()

        verify {
            walletConnectDeepLinkFactory.create(eq(mockedUri))
        }
    }

    @Test
    fun `launchDeepLink ignores unknown scheme`() = runTest {
        every { mockedUri.scheme } returns "https"
        every { mockedUri.host } returns "example.com"

        deepLinkFactory.handleDeeplink(mockedUri, testScope)

        advanceUntilIdle()

        verify(inverse = true) {
            onrampDeepLinkFactory.create(any(), any())
            sellDeepLinkFactory.create(any(), any())
            buyDeepLinkFactory.create(any())
            referralDeepLinkFactory.create()
            walletConnectDeepLinkFactory.create(any())
            walletDeepLinkFactory.create()
            tokenDetailsDeepLinkFactory.create(any(), any())
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
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value"))) }

        // Test Sell
        every { mockedUri.host } returns "redirect_sell"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { sellDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value"))) }

        // Test Token Details
        every { mockedUri.host } returns "token"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { tokenDetailsDeepLinkFactory.create(eq(testScope), eq(mapOf("param" to "value"))) }

        // Reset params
        every { mockedUri.queryParameterNames } returns emptySet()
        every { mockedUri.getQueryParameter(any()) } returns ""

        // Test Buy
        every { mockedUri.host } returns "redirect"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { buyDeepLinkFactory.create(eq(testScope)) }

        // Test Referral
        every { mockedUri.host } returns "referral"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { referralDeepLinkFactory.create() }

        // Test Wallet
        every { mockedUri.host } returns "main"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { walletDeepLinkFactory.create() }
    }

    @Test
    fun `handleTangemDeepLinks incorrect host`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "unknown"

        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope)

        advanceUntilIdle()

        verify(inverse = true) {
            onrampDeepLinkFactory.create(any(), any())
            sellDeepLinkFactory.create(any(), any())
            buyDeepLinkFactory.create(any())
            referralDeepLinkFactory.create()
            walletConnectDeepLinkFactory.create(any())
            walletDeepLinkFactory.create()
            tokenDetailsDeepLinkFactory.create(any(), any())
        }
    }

    @Test
    fun `getParams filters malicious parameters`() = runTest {
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"
        every { mockedUri.query } returns "safe=ok&malicious=%3Cscript%3E&quote=O%27Brien"
        every { mockedUri.queryParameterNames } returns setOf("safe", "malicious", "quote")
        every { mockedUri.getQueryParameter("safe") } returns "ok"
        every { mockedUri.getQueryParameter("malicious") } returns "<script>"
        every { mockedUri.getQueryParameter("quote") } returns "O'Brien"

        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
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
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("safe" to "ok"))) }

        every { mockedUri.query } returns "param123=ok"
        every { mockedUri.queryParameterNames } returns setOf("param123")
        every { mockedUri.getQueryParameter("param123") } returns "ok"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(mapOf("param123" to "ok"))) }

        every { mockedUri.query } returns "unsafe=<script>"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "<script>"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }

        every { mockedUri.query } returns "unsafe=O'Brien"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "O'Brien"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }

        every { mockedUri.query } returns "unsafe=test;"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "test;"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }

        every { mockedUri.query } returns "unsafe=test+attack"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "test+attack"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }

        every { mockedUri.query } returns "unsafe=test\\path"
        every { mockedUri.queryParameterNames } returns setOf("unsafe")
        every { mockedUri.getQueryParameter("unsafe") } returns "test\\path"
        deepLinkFactory.handleDeeplink(mockedUri, testScope)
        advanceUntilIdle()
        verify { onrampDeepLinkFactory.create(eq(testScope), eq(emptyMap())) }
    }
}