package com.tangem.tap.routing.utils

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.any
import io.mockk.capture
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DeepLinkFactoryTest {

    private val cardSdkProvider = mockk<CardSdkProvider>(relaxed = true) {
        every { sdk.uiVisibility() } returns MutableStateFlow(false)
    }
    private val urlOpener = mockk<UrlOpener>(relaxed = true)
    private val marketingFeatureToggles = mockk<MarketingFeatureToggles> {
        every { isUidBindingEnabled } returns false
    }
    private val marketingUidBindingRepository = mockk<MarketingUidBindingRepository>(relaxed = true)
    private val registeredHandler = mockk<DeepLinkHandler>(relaxed = true) {
        every { isEnabled } returns true
        every { isOpenableFromPush } returns true
    }
    private val deepLinkFactory = DeepLinkFactory(
        cardSdkProvider = cardSdkProvider,
        urlOpener = urlOpener,
        marketingFeatureToggles = marketingFeatureToggles,
        marketingUidBindingRepository = marketingUidBindingRepository,
        appCoroutineScope = TestAppCoroutineScope(),
        deepLinkHandlers = mapOf("tangem://main" to registeredHandler),
    )

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
        clearMocks(registeredHandler, urlOpener)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()
    }

    @Test
    fun `GIVEN registered deep link WHEN app is ready THEN handler receives validated request`() = runTest {
        // Arrange
        val uri = uri(
            value = "tangem://main?param=value",
            scheme = "tangem",
            host = "main",
            queryParameters = mapOf("param" to "value"),
        )
        val request = slot<DeepLinkRequest>()

        // Act
        deepLinkFactory.handleDeeplink(uri, testScope, isFromOnNewIntent = true)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        advanceUntilIdle()

        // Assert
        verify { registeredHandler.handle(capture(request)) }
        assertThat(request.captured.uri).isEqualTo(uri)
        assertThat(request.captured.queryParams).isEqualTo(mapOf("param" to "value"))
        assertThat(request.captured.scope).isEqualTo(testScope)
        assertThat(request.captured.isFromOnNewIntent).isTrue()
    }

    @Test
    fun `GIVEN app is not ready WHEN deep link arrives THEN handler is not called`() = runTest {
        // Arrange
        val uri = uri(value = "tangem://main", scheme = "tangem", host = "main")

        // Act
        deepLinkFactory.handleDeeplink(uri, testScope, isFromOnNewIntent = false)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Initial)
        advanceUntilIdle()

        // Assert
        verify(inverse = true) { registeredHandler.handle(any()) }
    }

    @Test
    fun `GIVEN push deep link handler is not openable WHEN deep link arrives THEN request is consumed`() = runTest {
        // Arrange
        every { registeredHandler.isOpenableFromPush } returns false
        val uri = uri(value = "tangem://main", scheme = "tangem", host = "main")

        // Act
        deepLinkFactory.handleDeeplink(uri, testScope, isFromOnNewIntent = false, source = DeeplinkSource.Push)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        advanceUntilIdle()

        // Assert
        verify(inverse = true) { registeredHandler.handle(any()) }
        verify(inverse = true) { urlOpener.openUrl(any()) }
    }

    @Test
    fun `GIVEN push deep link has no registered handler WHEN app is ready THEN trusted web link opens`() = runTest {
        // Arrange
        val uri = uri(
            value = "https://tangem.com/unknown",
            scheme = "https",
            host = "tangem.com",
            pathSegments = listOf("unknown"),
        )

        // Act
        deepLinkFactory.handleDeeplink(uri, testScope, isFromOnNewIntent = false, source = DeeplinkSource.Push)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        advanceUntilIdle()

        // Assert
        verify { urlOpener.openUrl("https://tangem.com/unknown") }
    }

    @Test
    fun `GIVEN external Tangem Pay main link WHEN deep link arrives THEN request is ignored`() = runTest {
        // Arrange
        val uri = uri(
            value = "https://tangem.com/pay-app-main",
            scheme = "https",
            host = "tangem.com",
            path = "/pay-app-main",
            pathSegments = listOf("pay-app-main"),
        )

        // Act
        deepLinkFactory.handleDeeplink(uri, testScope, isFromOnNewIntent = false)
        deepLinkFactory.checkRoutingReadiness(AppRoute.Wallet)
        advanceUntilIdle()

        // Assert
        verify(inverse = true) { urlOpener.openUrl(any()) }
        verify(inverse = true) { registeredHandler.handle(any()) }
    }

    private fun uri(
        value: String,
        scheme: String,
        host: String?,
        path: String? = null,
        pathSegments: List<String> = emptyList(),
        queryParameters: Map<String, String> = emptyMap(),
    ): Uri = mockk(relaxed = true) {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.host } returns host
        every { this@mockk.path } returns path
        every { this@mockk.pathSegments } returns pathSegments
        every { this@mockk.isOpaque } returns false
        every { this@mockk.queryParameterNames } returns queryParameters.keys
        queryParameters.forEach { (name, parameter) ->
            every { getQueryParameter(name) } returns parameter
        }
        every { this@mockk.userInfo } returns null
        every { toString() } returns value
    }
}