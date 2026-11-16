package com.tangem.tap.routing.utils

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import javax.inject.Provider

internal class RootDeepLinkDispatcherTest {

    private val isCardSdkVisible = MutableStateFlow(false)
    private val cardSdkProvider = mockk<CardSdkProvider>(relaxed = true) {
        every { sdk.uiVisibility() } returns isCardSdkVisible
    }
    private val urlOpener = mockk<UrlOpener>(relaxed = true)

    private val registeredWalletHandler = createHandler()
    private val registeredJointAccountInviteHandler = createHandler()
    private val registeredWalletConnectHandler = createHandler()
    private val registeredTangemPayMainHandler = createHandler()

    private val deepLinkHandlerRegistry = DeepLinkHandlerRegistry(
        handlers = mapOf(
            "tangem://main" to registeredWalletHandler,
            "https://tangem.com/joint-account/join" to registeredJointAccountInviteHandler,
            "wc:" to registeredWalletConnectHandler,
            "tangem://pay-app-main" to registeredTangemPayMainHandler,
            "https://tangem.com/pay-app-main" to registeredTangemPayMainHandler,
        ).mapValues { (_, handler) -> Provider { handler } },
    )

    private val deepLinkFactory = DeepLinkFactory(
        marketingFeatureToggles = mockk(),
        marketingUidBindingRepository = mockk(),
        appCoroutineScope = TestAppCoroutineScope(),
        deepLinkHandlerRegistry = deepLinkHandlerRegistry,
    )

    private val dispatcher = RootDeepLinkDispatcher(
        deepLinkFactory = deepLinkFactory,
        deepLinkHandlerRegistry = deepLinkHandlerRegistry,
        cardSdkProvider = cardSdkProvider,
        urlOpener = urlOpener,
    )

    private val currentRoute = MutableStateFlow<AppRoute?>(null)
    private val router = mockk<Router>()
    private val requestScope = mockk<CoroutineScope>()
    private val mockedUri = mockk<Uri>(relaxed = true)
    private val isFromOnNewIntent: Boolean = false

    @BeforeEach
    fun setUp() {
        every { mockedUri.path } returns "/path"
        every { mockedUri.toString() } returns "https://example.com/path?query=param"
        every { mockedUri.authority } returns "example.com"
        every { mockedUri.port } returns 443 // Default HTTPS port
        every { mockedUri.fragment } returns null // No fragment in this URI
        every { mockedUri.userInfo } returns null
    }

    @Test
    fun `GIVEN parked deep link WHEN route becomes ready THEN handler gets the root router`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        // Act
        dispatchDeepLink()

        // Assert
        verify {
            registeredWalletHandler.handle(
                DeepLinkRequest(
                    uri = mockedUri,
                    queryParams = mapOf("param" to "value"),
                    scope = requestScope,
                    isFromOnNewIntent = isFromOnNewIntent,
                    router = router,
                ),
            )
        }
    }

    @Test
    fun `GIVEN ready route WHEN deep link arrives THEN it is routed`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns emptySet()
        currentRoute.value = AppRoute.Wallet
        dispatcher.observe(backgroundScope, currentRoute, router)
        runCurrent()

        // Act
        deepLinkFactory.handleDeeplink(mockedUri, requestScope, isFromOnNewIntent)
        runCurrent()

        // Assert
        verify(exactly = 1) { registeredWalletHandler.handle(any()) }
    }

    @Test
    fun `GIVEN routing not permitted WHEN deep link parked THEN deep link is not routed`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns emptySet()

        // Act
        dispatchDeepLink(route = AppRoute.Initial)

        // Assert
        verify(inverse = true) { registeredWalletHandler.handle(any()) }
    }

    @Test
    fun `GIVEN card scan visible WHEN deep link parked THEN deep link is not routed`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns emptySet()
        isCardSdkVisible.value = true

        // Act
        dispatchDeepLink()

        // Assert
        verify(inverse = true) { registeredWalletHandler.handle(any()) }
    }

    @Test
    fun `GIVEN dispatched deep link WHEN route changes THEN it is not dispatched again`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.queryParameterNames } returns emptySet()
        dispatchDeepLink()

        // Act
        currentRoute.value = AppRoute.AppSettings
        runCurrent()

        // Assert
        verify(exactly = 1) { registeredWalletHandler.handle(any()) }
    }

    @ParameterizedTest
    @MethodSource("provideQueryParamModels")
    fun `GIVEN query param WHEN deep link dispatched THEN only a safe one reaches the handler`(model: QueryParamModel) =
        runTest {
            // Arrange
            every { mockedUri.scheme } returns "tangem"
            every { mockedUri.host } returns "main"
            every { mockedUri.queryParameterNames } returns setOf(model.name)
            every { mockedUri.getQueryParameter(model.name) } returns model.value

            // Act
            dispatchDeepLink()

            // Assert
            val expectedParams = if (model.isKept) mapOf(model.name to model.value) else emptyMap()
            verify { registeredWalletHandler.handle(match { it.queryParams == expectedParams }) }
        }

    data class QueryParamModel(val name: String, val value: String, val isKept: Boolean)

    @Test
    fun `GIVEN https path registered WHEN deep link dispatched THEN registered handler is used`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "https"
        every { mockedUri.host } returns "tangem.com"
        every { mockedUri.path } returns "/joint-account/join"
        every { mockedUri.pathSegments } returns listOf("joint-account", "join")
        every { mockedUri.queryParameterNames } returns emptySet()

        // Act
        dispatchDeepLink()

        // Assert
        verify { registeredJointAccountInviteHandler.handle(any()) }
    }

    @Test
    fun `GIVEN opaque uri WHEN deep link dispatched THEN scheme handler gets empty query`() = runTest {
        // Arrange
        every { mockedUri.isOpaque } returns true
        every { mockedUri.scheme } returns "wc"
        every { mockedUri.host } returns null
        every { mockedUri.path } returns null
        every { mockedUri.queryParameterNames } throws UnsupportedOperationException("This isn't a hierarchical URI.")

        // Act
        dispatchDeepLink()

        // Assert
        verify { registeredWalletConnectHandler.handle(match { it.queryParams.isEmpty() }) }
    }

    @Test
    fun `GIVEN handler not openable from push WHEN push link THEN consumed without handling`() = runTest {
        // Arrange
        every { registeredWalletConnectHandler.isOpenableFromPush } returns false
        every { mockedUri.isOpaque } returns true
        every { mockedUri.scheme } returns "wc"
        every { mockedUri.host } returns null
        every { mockedUri.toString() } returns "wc:abc@2?relay-protocol=irn"

        // Act
        dispatchDeepLink(source = DeeplinkSource.Push)

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
        dispatchDeepLink(source = DeeplinkSource.External)

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
        dispatchDeepLink(source = DeeplinkSource.Push)

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
        dispatchDeepLink(source = DeeplinkSource.Push)

        // Assert
        verify(inverse = true) { registeredJointAccountInviteHandler.handle(any()) }
        verify { urlOpener.openUrl("https://tangem.com/joint-account/join/invite-id") }
    }

    @Test
    fun `GIVEN host not registered WHEN deep link dispatched THEN deep link is not routed`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "onramp"
        every { mockedUri.queryParameterNames } returns setOf("param")
        every { mockedUri.getQueryParameter("param") } returns "value"

        // Act
        dispatchDeepLink()

        // Assert
        verify(inverse = true) {
            registeredWalletHandler.handle(any())
            registeredJointAccountInviteHandler.handle(any())
        }
    }

    @ParameterizedTest
    @MethodSource("provideWebFallbackModels")
    fun `GIVEN push link that matches no route WHEN deep link dispatched THEN opens it only when the host is trusted`(
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
        dispatchDeepLink(source = model.source)

        // Assert
        if (model.isOpenedInBrowser) {
            verify { urlOpener.openUrl(model.url) }
        } else {
            verify(inverse = true) { urlOpener.openUrl(any()) }
        }
    }

    @Test
    fun `GIVEN routable deeplink from push WHEN deep link dispatched THEN routes it and opens no browser`() = runTest {
        // Arrange
        every { mockedUri.scheme } returns "tangem"
        every { mockedUri.host } returns "main"
        every { mockedUri.query } returns null
        every { mockedUri.queryParameterNames } returns emptySet()
        every { mockedUri.toString() } returns "tangem://main"

        // Act
        dispatchDeepLink(source = DeeplinkSource.Push)

        // Assert
        verify { registeredWalletHandler.handle(any()) }
        verify(inverse = true) { urlOpener.openUrl(any()) }
    }

    @ParameterizedTest
    @MethodSource("providePayAppMainModels")
    fun `GIVEN pay-app-main link WHEN deep link dispatched THEN handled only from push`(model: PayAppMainModel) =
        runTest {
            // Arrange
            every { mockedUri.scheme } returns model.scheme
            every { mockedUri.host } returns model.host
            every { mockedUri.path } returns model.path
            every { mockedUri.pathSegments } returns model.pathSegments
            every { mockedUri.queryParameterNames } returns emptySet()

            // Act
            dispatchDeepLink(source = model.source)

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
            dispatcher.observe(backgroundScope, currentRoute, router)
            deepLinkFactory.handleDeeplink(pushUri, requestScope, isFromOnNewIntent, DeeplinkSource.Push)

            // Act
            deepLinkFactory.handleDeeplink(mockedUri, requestScope, isFromOnNewIntent, DeeplinkSource.External)
            currentRoute.value = AppRoute.Wallet
            runCurrent()

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

    data class WebFallbackModel(
        val scheme: String,
        val host: String,
        val path: String,
        val url: String,
        val source: DeeplinkSource,
        val isOpenedInBrowser: Boolean,
        val userInfo: String? = null,
    )

    /** Parks [mockedUri] and then makes [route] the active one, as the root does once its stack is resolved. */
    private fun TestScope.dispatchDeepLink(
        source: DeeplinkSource = DeeplinkSource.External,
        route: AppRoute = AppRoute.Wallet,
    ) {
        dispatcher.observe(backgroundScope, currentRoute, router)
        deepLinkFactory.handleDeeplink(mockedUri, requestScope, isFromOnNewIntent, source)
        currentRoute.value = route
        runCurrent()
    }

    private fun createHandler(): DeepLinkHandler = mockk(relaxed = true) {
        every { isEnabled } returns true
        every { isOpenableFromPush } returns true
    }

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