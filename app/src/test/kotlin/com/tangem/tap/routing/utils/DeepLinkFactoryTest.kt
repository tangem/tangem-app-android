package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
internal class DeepLinkFactoryTest {

    private val cardSdkProvider = mockk<CardSdkProvider>(relaxed = true) {
        every { sdk.uiVisibility() } returns MutableStateFlow(false)
    }

    private val urlOpener = mockk<UrlOpener>(relaxed = true)

    private val marketingFeatureToggles = mockk<MarketingFeatureToggles>()
    private val marketingUidBindingRepository = mockk<MarketingUidBindingRepository>(relaxed = true)
    private val registeredWalletHandler = mockk<DeepLinkHandler>(relaxed = true) {
        every { isEnabled } returns true
        every { isOpenableFromPush } returns true
    }
    private val registeredJointAccountInviteHandler = mockk<DeepLinkHandler>(relaxed = true) {
        every { isEnabled } returns true
        every { isOpenableFromPush } returns true
    }
    private val registeredWalletConnectHandler = mockk<DeepLinkHandler>(relaxed = true) {
        every { isEnabled } returns true
        every { isOpenableFromPush } returns true
    }
    private val registeredTangemPayMainHandler = mockk<DeepLinkHandler>(relaxed = true) {
        every { isEnabled } returns true
        every { isOpenableFromPush } returns true
    }

    private val mockedUri = mockk<Uri>(relaxed = true)
    private val isFromOnNewIntent: Boolean = false

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    private val deepLinkFactory = DeepLinkFactory(
        cardSdkProvider = cardSdkProvider,
        urlOpener = urlOpener,
        marketingFeatureToggles = marketingFeatureToggles,
        marketingUidBindingRepository = marketingUidBindingRepository,
        appCoroutineScope = TestAppCoroutineScope(),
        deepLinkHandlerRegistry = DeepLinkHandlerRegistry(
            handlers = mapOf(
                "tangem://main" to registeredWalletHandler,
                "https://tangem.com/joint-account/join" to registeredJointAccountInviteHandler,
                "wc:" to registeredWalletConnectHandler,
                "tangem://pay-app-main" to registeredTangemPayMainHandler,
                "https://tangem.com/pay-app-main" to registeredTangemPayMainHandler,
            ).mapValues { (_, handler) -> Provider { handler } },
        ),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)

        Dispatchers.setMain(testDispatcher)

        every { mockedUri.path } returns "/path"
        every { mockedUri.toString() } returns "https://example.com/path?query=param"
        every { mockedUri.authority } returns "example.com"
        every { mockedUri.port } returns 443 // Default HTTPS port
        every { mockedUri.fragment } returns null // No fragment in this URI
        every { mockedUri.userInfo } returns null

        clearMocks(marketingUidBindingRepository)
        every { marketingFeatureToggles.isUidBindingEnabled } returns true
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        // Reset the main dispatcher
        Dispatchers.resetMain()
        // Clean up test coroutines
        testScope.cancel()
    }

    @Test
    fun `GIVEN routing permitted WHEN handleDeeplink THEN parked deep link is routed`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        // Act
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        advanceUntilIdle()

        // Assert
        verify {
            registeredWalletHandler.handle(
                DeepLinkRequest(
                    uri = mockedUri,
                    queryParams = mapOf("param" to "value"),
                    scope = testScope,
                    isFromOnNewIntent = isFromOnNewIntent,
                ),
            )
        }
    }

    @Test
    fun `GIVEN routing not permitted WHEN handleDeeplink THEN deep link is not routed`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns emptySet()

        // Act
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Initial)
        advanceUntilIdle()

        // Assert
        verify(inverse = true) { registeredWalletHandler.handle(any()) }
    }

    @Test
    fun `GIVEN card scan visible WHEN handleDeeplink THEN deep link is not routed`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns emptySet()
        every { cardSdkProvider.sdk.uiVisibility() } returns MutableStateFlow(true)

        // Act
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        advanceUntilIdle()

        // Assert
        verify(inverse = true) { registeredWalletHandler.handle(any()) }
    }

    @ParameterizedTest
    @MethodSource("provideQueryParamModels")
    fun `GIVEN query param WHEN handleDeeplink THEN only a safe one reaches the handler`(model: QueryParamModel) =
        runTest {
            // Arrange
            every { mockedUri.scheme } returns "tangem"
            every { mockedUri.host } returns "main"
            every { mockedUri.queryParameterNames } returns setOf(model.name)
            every { mockedUri.getQueryParameter(model.name) } returns model.value

            // Act
            deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
            deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
            advanceUntilIdle()

            // Assert
            val expectedParams = if (model.isKept) mapOf(model.name to model.value) else emptyMap()
            verify {
                registeredWalletHandler.handle(
                    DeepLinkRequest(
                        uri = mockedUri,
                        queryParams = expectedParams,
                        scope = testScope,
                        isFromOnNewIntent = isFromOnNewIntent,
                    ),
                )
            }
        }

    data class QueryParamModel(val name: String, val value: String, val isKept: Boolean)

    @Test
    fun `GIVEN host registered WHEN handle THEN registered handler gets request`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()

        // Assert
        verify {
            registeredWalletHandler.handle(
                DeepLinkRequest(
                    uri = mockedUri,
                    queryParams = mapOf("param" to "value"),
                    scope = testScope,
                    isFromOnNewIntent = isFromOnNewIntent,
                ),
            )
        }
    }

    @Test
    fun `GIVEN https path registered WHEN handle THEN registered handler is used`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "https"
        every { mockedUri.host } returns "tangem.com"
        every { mockedUri.path } returns "/joint-account/join"
        every { mockedUri.pathSegments } returns listOf("joint-account", "join")
        every { mockedUri.queryParameterNames } returns emptySet()

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()

        // Assert
        verify { registeredJointAccountInviteHandler.handle(any()) }
    }

    @Test
    fun `GIVEN opaque uri WHEN handle THEN scheme handler gets empty query`() = runTest {
        // Arrange
        every { mockedUri.isOpaque } returns true
        every { mockedUri.scheme } returns "wc"
        every { mockedUri.host } returns null
        every { mockedUri.path } returns null
        every { mockedUri.queryParameterNames } throws UnsupportedOperationException("This isn't a hierarchical URI.")

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()

        // Assert
        verify {
            registeredWalletConnectHandler.handle(
                DeepLinkRequest(
                    uri = mockedUri,
                    queryParams = emptyMap(),
                    scope = testScope,
                    isFromOnNewIntent = isFromOnNewIntent,
                ),
            )
        }
    }

    @Test
    fun `GIVEN handler not openable from push WHEN push link THEN consumed without handling`() =
        runTest {
            // Arrange
            every { registeredWalletConnectHandler.isOpenableFromPush } returns false
            every { mockedUri.isOpaque } returns true
            every { mockedUri.scheme } returns "wc"
            every { mockedUri.host } returns null
            every { mockedUri.toString() } returns "wc:abc@2?relay-protocol=irn"

            // Act
            deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
            deepLinkFactory.handleDeeplink(
                deeplinkUri = mockedUri,
                coroutineScope = testScope,
                isFromOnNewIntent = isFromOnNewIntent,
                source = DeeplinkSource.Push,
            )
            advanceUntilIdle()

            // Assert
            verify(inverse = true) {
                registeredWalletConnectHandler.handle(any())
                urlOpener.openUrl(any())
            }
        }

    @Test
    fun `GIVEN handler not openable from push WHEN external link THEN handled`() = runTest {
        // Arrange
        every { registeredWalletConnectHandler.isOpenableFromPush } returns false
        every { mockedUri.isOpaque } returns true
        every { mockedUri.scheme } returns "wc"
        every { mockedUri.host } returns null

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(
            deeplinkUri = mockedUri,
            coroutineScope = testScope,
            isFromOnNewIntent = isFromOnNewIntent,
            source = DeeplinkSource.External,
        )
        advanceUntilIdle()

        // Assert
        verify { registeredWalletConnectHandler.handle(any()) }
    }

    @Test
    fun `GIVEN handler openable from push WHEN push link THEN handled`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns emptySet()

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(
            deeplinkUri = mockedUri,
            coroutineScope = testScope,
            isFromOnNewIntent = isFromOnNewIntent,
            source = DeeplinkSource.Push,
        )
        advanceUntilIdle()

        // Assert
        verify { registeredWalletHandler.handle(any()) }
    }

    @Test
    fun `GIVEN handler disabled WHEN push https link THEN opened in browser`() = runTest {
        // Arrange
        every { registeredJointAccountInviteHandler.isEnabled } returns false
        every { mockedUri.scheme } returns "https"
        every { mockedUri.host } returns "tangem.com"
        every { mockedUri.path } returns "/joint-account/join/invite-id"
        every { mockedUri.pathSegments } returns listOf("joint-account", "join", "invite-id")
        every { mockedUri.queryParameterNames } returns emptySet()
        every { mockedUri.toString() } returns "https://tangem.com/joint-account/join/invite-id"

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(
            deeplinkUri = mockedUri,
            coroutineScope = testScope,
            isFromOnNewIntent = isFromOnNewIntent,
            source = DeeplinkSource.Push,
        )
        advanceUntilIdle()

        // Assert
        verify(inverse = true) { registeredJointAccountInviteHandler.handle(any()) }
        verify { urlOpener.openUrl("https://tangem.com/joint-account/join/invite-id") }
    }

    @Test
    fun `GIVEN host not registered WHEN handle THEN deep link is not routed`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent)
        advanceUntilIdle()

        // Assert
        verify(inverse = true) {
            registeredWalletHandler.handle(any())
            registeredJointAccountInviteHandler.handle(any())
        }
    }

    @ParameterizedTest
    @MethodSource("provideWebFallbackModels")
    fun `GIVEN push link that matches no route WHEN handleDeeplink THEN opens it only when the host is trusted`(
        model: WebFallbackModel,
    ) = runTest {
        // Arrange
        every { mockedUri.scheme } returns model.scheme
        every { mockedUri.host } returns model.host
        every { mockedUri.userInfo } returns model.userInfo
        every { mockedUri.path } returns model.path
        every { mockedUri.queryParameterNames } returns emptySet()
        every { mockedUri.toString() } returns model.url

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent, model.source)
        advanceUntilIdle()

        // Assert
        if (model.isOpenedInBrowser) {
            verify { urlOpener.openUrl(model.url) }
        } else {
            verify(inverse = true) { urlOpener.openUrl(any()) }
        }
    }

    @Test
    fun `GIVEN routable deeplink from push WHEN handleDeeplink THEN routes it and opens no browser`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.query } returns null
        every { mockedUri.queryParameterNames } returns emptySet()
        every { mockedUri.toString() } returns "tangem://main"

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent, DeeplinkSource.Push)
        advanceUntilIdle()

        // Assert
        verify { registeredWalletHandler.handle(any()) }
        verify(inverse = true) { urlOpener.openUrl(any()) }
    }

    @ParameterizedTest
    @MethodSource("providePayAppMainModels")
    fun `GIVEN pay-app-main link WHEN handleDeeplink THEN handled only from push`(
        model: PayAppMainModel,
    ) = runTest {
        // Arrange
        every { mockedUri.scheme } returns model.scheme
        every { mockedUri.host } returns model.host
        every { mockedUri.path } returns model.path
        every { mockedUri.pathSegments } returns model.pathSegments
        every { mockedUri.queryParameterNames } returns emptySet()

        // Act
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent, model.source)
        advanceUntilIdle()

        // Assert
        if (model.isHandled) {
            verify { registeredTangemPayMainHandler.handle(any()) }
        } else {
            verify(inverse = true) { registeredTangemPayMainHandler.handle(any()) }
        }
    }

    @Test
    fun `GIVEN parked push deep link WHEN external pay-app-main link arrives THEN parked one is still routed`() =
        runTest {
            // Arrange
            val pushUri = mockk<Uri>(relaxed = true) {
                every { scheme } returns "tangem"
                every { host } returns "main"
                every { queryParameterNames } returns emptySet()
            }
            every { mockedUri.scheme } returns "tangem"
            every { mockedUri.host } returns "pay-app-main"
            deepLinkFactory.handleDeeplink(pushUri, testScope, isFromOnNewIntent, DeeplinkSource.Push)

            // Act
            deepLinkFactory.handleDeeplink(mockedUri, testScope, isFromOnNewIntent, DeeplinkSource.External)
            deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
            advanceUntilIdle()

            // Assert
            verify { registeredWalletHandler.handle(any()) }
            verify(inverse = true) { registeredTangemPayMainHandler.handle(any()) }
        }

    data class PayAppMainModel(
        val scheme: String,
        val host: String,
        val path: String,
        val pathSegments: List<String>,
        val source: DeeplinkSource,
        val isHandled: Boolean,
    )

    @Test
    fun `GIVEN link to a known target WHEN bindMarketingUid THEN the first non-empty uid is bound`() {
        // Arrange
        val uri = marketingUidUri(host = "main", uids = listOf("", "spring-campaign", "other"))

        // Act
        deepLinkFactory.bindMarketingUid(uri)

        // Assert
        coVerify(exactly = 1) { marketingUidBindingRepository.bind("spring-campaign") }
    }

    @ParameterizedTest
    @MethodSource("provideNotBoundMarketingUidModels")
    fun `GIVEN link without a bindable uid WHEN bindMarketingUid THEN nothing is bound`(
        model: NotBoundMarketingUidModel,
    ) {
        // Arrange
        every { marketingFeatureToggles.isUidBindingEnabled } returns model.isToggleOn
        val uri = marketingUidUri(scheme = model.scheme, host = model.host, uids = model.uids)

        // Act
        deepLinkFactory.bindMarketingUid(uri)

        // Assert
        coVerify(exactly = 0) { marketingUidBindingRepository.bind(any()) }
    }

    private fun marketingUidUri(scheme: String = "tangem", host: String?, uids: List<String>): Uri {
        val isOpaque = host == null
        return mockk(relaxed = true) {
            every { this@mockk.scheme } returns scheme
            every { this@mockk.host } returns host
            every { pathSegments } returns emptyList()
            every { this@mockk.isOpaque } returns isOpaque
            if (isOpaque) {
                every { getQueryParameters(any()) } throws UnsupportedOperationException()
            } else {
                every { getQueryParameters("uid") } returns uids
            }
        }
    }

    data class NotBoundMarketingUidModel(
        val name: String,
        val scheme: String = "tangem",
        val host: String? = "main",
        val uids: List<String> = listOf("spring-campaign"),
        val isToggleOn: Boolean = true,
    ) {
        override fun toString(): String = name
    }

    data class WebFallbackModel(
        val scheme: String,
        val host: String,
        val path: String,
        val url: String,
        val source: DeeplinkSource,
        val isOpenedInBrowser: Boolean,
        val userInfo: String? = null,
    )

    private companion object {

        @JvmStatic
        fun providePayAppMainModels(): List<PayAppMainModel> = listOf(
            PayAppMainModel(
                scheme = "tangem",
                host = "pay-app-main",
                path = "",
                pathSegments = emptyList(),
                source = DeeplinkSource.Push,
                isHandled = true,
            ),
            PayAppMainModel(
                scheme = "tangem",
                host = "pay-app-main",
                path = "",
                pathSegments = emptyList(),
                source = DeeplinkSource.External,
                isHandled = false,
            ),
            PayAppMainModel(
                scheme = "https",
                host = "tangem.com",
                path = "//pay-app-main",
                pathSegments = listOf("pay-app-main"),
                source = DeeplinkSource.Push,
                isHandled = true,
            ),
            PayAppMainModel(
                scheme = "https",
                host = "tangem.com",
                path = "//pay-app-main",
                pathSegments = listOf("pay-app-main"),
                source = DeeplinkSource.External,
                isHandled = false,
            ),
            PayAppMainModel(
                scheme = "https",
                host = "tangem.com",
                path = "/pay-app-main/details",
                pathSegments = listOf("pay-app-main", "details"),
                source = DeeplinkSource.External,
                isHandled = false,
            ),
            PayAppMainModel(
                scheme = "https",
                host = "tangem.com",
                path = "/pay-app-main/x",
                pathSegments = listOf("pay-app-main/x"),
                source = DeeplinkSource.External,
                isHandled = false,
            ),
        )

        @JvmStatic
        fun provideNotBoundMarketingUidModels(): List<NotBoundMarketingUidModel> = listOf(
            NotBoundMarketingUidModel(name = "toggle off", isToggleOn = false),
            NotBoundMarketingUidModel(name = "no uid", uids = emptyList()),
            NotBoundMarketingUidModel(name = "empty uid", uids = listOf("")),
            NotBoundMarketingUidModel(name = "unknown target", host = "not_a_route"),
            NotBoundMarketingUidModel(name = "opaque wc link", scheme = "wc", host = null),
        )

        @JvmStatic
        fun provideQueryParamModels(): List<QueryParamModel> = listOf(
            QueryParamModel(name = "safe", value = "ok", isKept = true),
            QueryParamModel(name = "param123", value = "ok", isKept = true),
            QueryParamModel(name = "unsafe", value = "<script>", isKept = false),
            QueryParamModel(name = "unsafe", value = "test;", isKept = false),
            QueryParamModel(name = "unsafe", value = "test+attack", isKept = false),
            QueryParamModel(name = "unsafe", value = "test\\path", isKept = false),
        )

        @JvmStatic
        fun provideWebFallbackModels(): List<WebFallbackModel> = listOf(
            // The [REDACTED_TASK_KEY] marketing shape: a trusted host on a path no handler claims
            WebFallbackModel(
                scheme = "https",
                host = "tangem.com",
                path = "/pricing/",
                url = "https://tangem.com/pricing/?utm_campaign=pizzaday",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = true,
            ),
            // `+` in a query is an encoded space — CIO/AppsFlyer campaign builders produce it routinely,
            // and the fallback must not reject an already-trusted, already-parsed URL over it
            WebFallbackModel(
                scheme = "https",
                host = "tangem.com",
                path = "/pricing/",
                url = "https://tangem.com/pricing/?utm_content=summer+sale",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = true,
            ),
            // Hostnames are case-insensitive per RFC 3986
            WebFallbackModel(
                scheme = "https",
                host = "TANGEM.COM",
                path = "/pricing/",
                url = "https://TANGEM.COM/pricing/",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = true,
            ),
            // AppsFlyer OneLink: must leave the app so the SDK can resolve attribution on the way back in
            WebFallbackModel(
                scheme = "https",
                host = "join.tangem.com",
                path = "/abc",
                url = "https://join.tangem.com/abc",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = true,
            ),
            // The OneLink carve-out is host-based, so it must not let a non-https scheme through
            WebFallbackModel(
                scheme = "tangem",
                host = "join.tangem.com",
                path = "/abc",
                url = "tangem://join.tangem.com/abc",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = false,
            ),
            WebFallbackModel(
                scheme = "https",
                host = "evil.com",
                path = "/phishing",
                url = "https://evil.com/phishing",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = false,
            ),
            // An unpatched android.net.Uri reads the host as tangem.com, a browser as evil.com
            WebFallbackModel(
                scheme = "https",
                host = "tangem.com",
                path = "/pricing/",
                url = "https://evil.com\\@tangem.com/pricing/",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = false,
                userInfo = "evil.com\\",
            ),
            WebFallbackModel(
                scheme = "https",
                host = "join.tangem.com",
                path = "/abc",
                url = "https://[REDACTED_EMAIL]/abc",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = false,
                userInfo = "user",
            ),
            // A custom scheme handed to ACTION_VIEW would bounce back in as an ungated intent.data.
            // The host must be one no handler claims, otherwise the fallback is never reached.
            WebFallbackModel(
                scheme = "tangem",
                host = "not_a_route",
                path = "/",
                url = "tangem://not_a_route",
                source = DeeplinkSource.Push,
                isOpenedInBrowser = false,
            ),
            // External: the system already routed this to us, so re-opening it would bounce endlessly
            WebFallbackModel(
                scheme = "https",
                host = "tangem.com",
                path = "/pricing/",
                url = "https://tangem.com/pricing/",
                source = DeeplinkSource.External,
                isOpenedInBrowser = false,
            ),
        )
    }
}