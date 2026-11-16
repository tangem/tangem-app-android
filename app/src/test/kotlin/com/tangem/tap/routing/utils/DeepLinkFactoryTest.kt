package com.tangem.tap.routing.utils

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import javax.inject.Provider

internal class DeepLinkFactoryTest {

    private val marketingUidBindingRepository = mockk<MarketingUidBindingRepository>(relaxed = true)
    private val walletHandler = mockk<DeepLinkHandler>(relaxed = true) {
        every { isEnabled } returns true
    }
    private val scope = mockk<CoroutineScope>()

    private val deepLinkFactory = DeepLinkFactory(
        marketingUidBindingRepository = marketingUidBindingRepository,
        appCoroutineScope = TestAppCoroutineScope(),
        deepLinkHandlerRegistry = DeepLinkHandlerRegistry(
            handlers = mapOf("tangem://main" to Provider { walletHandler }),
        ),
    )

    @Test
    fun `GIVEN deep link WHEN handleDeeplink THEN it is parked with its source and scope`() {
        // Arrange
        val uri = tangemUri(host = "main")

        // Act
        deepLinkFactory.handleDeeplink(uri, scope, isFromOnNewIntent = true, source = DeeplinkSource.Push)

        // Assert
        assertThat(deepLinkFactory.parkedDeepLink.value).isEqualTo(
            ParkedDeepLink(uri = uri, source = DeeplinkSource.Push, scope = scope, isFromOnNewIntent = true),
        )
    }

    @Test
    fun `GIVEN parked deep link WHEN another one arrives THEN it replaces the parked one`() {
        // Arrange
        val newUri = tangemUri(host = "main")
        deepLinkFactory.handleDeeplink(tangemUri(host = "main"), scope, isFromOnNewIntent = false)

        // Act
        deepLinkFactory.handleDeeplink(newUri, scope, isFromOnNewIntent = false)

        // Assert
        assertThat(deepLinkFactory.parkedDeepLink.value?.uri).isSameInstanceAs(newUri)
    }

    @Test
    fun `GIVEN parked deep link WHEN consume THEN it is taken once`() {
        // Arrange
        deepLinkFactory.handleDeeplink(tangemUri(host = "main"), scope, isFromOnNewIntent = false)
        val parked = requireNotNull(deepLinkFactory.parkedDeepLink.value)

        // Act
        val isFirstConsumed = deepLinkFactory.consume(parked)
        val isSecondConsumed = deepLinkFactory.consume(parked)

        // Assert
        assertThat(isFirstConsumed).isTrue()
        assertThat(isSecondConsumed).isFalse()
        assertThat(deepLinkFactory.parkedDeepLink.value).isNull()
    }

    @Test
    fun `GIVEN replaced deep link WHEN consume THEN the new one stays parked`() {
        // Arrange
        deepLinkFactory.handleDeeplink(tangemUri(host = "main"), scope, isFromOnNewIntent = false)
        val replaced = requireNotNull(deepLinkFactory.parkedDeepLink.value)
        deepLinkFactory.handleDeeplink(tangemUri(host = "main"), scope, isFromOnNewIntent = false)
        val parked = deepLinkFactory.parkedDeepLink.value

        // Act
        val isConsumed = deepLinkFactory.consume(replaced)

        // Assert
        assertThat(isConsumed).isFalse()
        assertThat(deepLinkFactory.parkedDeepLink.value).isSameInstanceAs(parked)
    }

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
        val uri = marketingUidUri(scheme = model.scheme, host = model.host, uids = model.uids)

        // Act
        deepLinkFactory.bindMarketingUid(uri)

        // Assert
        coVerify(exactly = 0) { marketingUidBindingRepository.bind(any()) }
    }

    private fun tangemUri(host: String): Uri = mockk(relaxed = true) {
        every { scheme } returns "tangem"
        every { this@mockk.host } returns host
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
    ) {
        override fun toString(): String = name
    }

    private companion object {

        @JvmStatic
        fun provideNotBoundMarketingUidModels(): List<NotBoundMarketingUidModel> = listOf(
            NotBoundMarketingUidModel(name = "no uid", uids = emptyList()),
            NotBoundMarketingUidModel(name = "empty uid", uids = listOf("")),
            NotBoundMarketingUidModel(name = "unknown target", host = "not_a_route"),
            NotBoundMarketingUidModel(name = "opaque wc link", scheme = "wc", host = null),
        )
    }
}