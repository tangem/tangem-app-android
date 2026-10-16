package com.tangem.tap.common.deeplink

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.deeplink.DeepLinkHandler
import com.tangem.common.routing.deeplink.DeepLinkHandlerRegistry
import com.tangem.tap.routing.utils.DeepLinkFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultQrDeepLinkMatcherTest {

    private val deepLinkFeatureToggles: DeepLinkFeatureToggles = mockk()

    private val matcher = DefaultQrDeepLinkMatcher(
        deepLinkHandlerRegistry = DeepLinkHandlerRegistry(
            handlers = listOf(
                "tangem://markets",
                "tangem://promo",
                "tangem://wc",
                "tangem://mcp.authorization",
                "tangem://redirect_sell",
                "tangem://redirect",
                "https://tangem.com/news",
                "wc:",
            ).associateWith { DeepLinkHandler {} },
        ),
        deepLinkFeatureToggles = deepLinkFeatureToggles,
    )

    private val urisByLink: Map<String, Uri> = provideTestModels().map(IsDeepLinkModel::link).associateWith(::createUri)

    @BeforeAll
    fun mockUriParsing() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers { urisByLink.getValue(firstArg()) }
    }

    @AfterAll
    fun unmockUriParsing() {
        unmockkStatic(Uri::class)
    }

    @BeforeEach
    fun setup() {
        clearMocks(deepLinkFeatureToggles)
        every { deepLinkFeatureToggles.isQrScanDeepLinksEnabled } returns true
    }

    @ParameterizedTest
    @ProvideTestModels
    fun isDeepLink(model: IsDeepLinkModel) {
        // Act
        val actual = matcher.isDeepLink(model.link)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN toggle disabled WHEN isDeepLink THEN screen link is not a deep link`() {
        // Arrange
        every { deepLinkFeatureToggles.isQrScanDeepLinksEnabled } returns false

        // Act
        val actual = matcher.isDeepLink(SCREEN_LINK)

        // Assert
        assertThat(actual).isFalse()
    }

    internal data class IsDeepLinkModel(val link: String, val expected: Boolean)

    private fun provideTestModels() = listOf(
        IsDeepLinkModel(link = SCREEN_LINK, expected = true),
        IsDeepLinkModel(link = "tangem://promo?promo_code=TEST123", expected = true),
        IsDeepLinkModel(link = "https://tangem.com/news/190801-polygon-protiv-ethereum", expected = true),
        // No feature claims the page, so it opens in the browser
        IsDeepLinkModel(link = "https://tangem.com/some/page", expected = true),
        IsDeepLinkModel(link = "https://www.tangem.com/some/page", expected = true),
        IsDeepLinkModel(link = "tangem://unknown", expected = false),
        IsDeepLinkModel(link = "tangem://wc?uri=wc:6ea45@2", expected = false),
        IsDeepLinkModel(link = "tangem://mcp.authorization?user_code=WDJB-MJHT", expected = false),
        IsDeepLinkModel(link = "tangem://redirect_sell?address=0xRecipient", expected = false),
        IsDeepLinkModel(link = "tangem://redirect", expected = false),
        IsDeepLinkModel(link = "wc:6ea45@2?relay-protocol=irn", expected = false),
        IsDeepLinkModel(link = "http://tangem.com/some/page", expected = false),
        IsDeepLinkModel(link = "https://example.com/page", expected = false),
        IsDeepLinkModel(link = "https://tangem.onelink.me/abcd", expected = false),
        IsDeepLinkModel(link = "bitcoin:bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq", expected = false),
    )

    // `authority`, not `host`: java.net.URI drops a host with an underscore (`redirect_sell`), android.net.Uri keeps it
    private fun createUri(link: String): Uri {
        val uri = URI(link)

        return mockk {
            every { scheme } returns uri.scheme
            every { host } returns uri.authority
            every { pathSegments } returns uri.path.orEmpty().split('/').filter(String::isNotEmpty)
        }
    }

    private companion object {
        const val SCREEN_LINK = "tangem://markets"
    }
}