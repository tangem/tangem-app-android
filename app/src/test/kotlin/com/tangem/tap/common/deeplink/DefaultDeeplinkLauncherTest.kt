package com.tangem.tap.common.deeplink

import android.content.Context
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.net.URI

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultDeeplinkLauncherTest {

    private val context: Context = mockk()
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsExceptionHandler: AnalyticsExceptionHandler = mockk(relaxed = true)

    private val launcher = DefaultDeeplinkLauncher(context, urlOpener, analyticsExceptionHandler)

    @BeforeAll
    fun mockUriParsing() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers { createUri(firstArg()) }
    }

    @AfterAll
    fun unmockUriParsing() {
        unmockkStatic(Uri::class)
    }

    @BeforeEach
    fun setup() {
        clearMocks(urlOpener, analyticsExceptionHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN link that can't be opened WHEN launch THEN exception sent to analytics`(model: UnopenableLinkModel) {
        // Arrange
        val event = slot<ExceptionAnalyticsEvent>()

        // Act
        launcher.launch(model.link)

        // Assert
        verify(exactly = 1) { analyticsExceptionHandler.sendException(capture(event)) }
        assertThat(event.captured.exception).isInstanceOf(model.expectedException)
        assertThat(event.captured.params).isEqualTo(mapOf("uri_scheme" to model.scheme, "uri_host" to model.host))
        verify(exactly = 0) { urlOpener.openUrl(any()) }
    }

    @Test
    fun `GIVEN reporting disabled WHEN launch link that can't be opened THEN nothing sent to analytics`() {
        // Act
        launcher.launch(link = "https://example.com/page", shouldReportFailure = false)

        // Assert
        verify(exactly = 0) { analyticsExceptionHandler.sendException(any()) }
        verify(exactly = 0) { urlOpener.openUrl(any()) }
    }

    internal data class UnopenableLinkModel(
        val link: String,
        val expectedException: Class<out UnopenableDeeplinkException>,
        val scheme: String,
        val host: String,
    )

    private fun provideTestModels() = listOf(
        UnopenableLinkModel(
            link = "https://example.com/page",
            expectedException = UntrustedDeeplinkException::class.java,
            scheme = "https",
            host = "example.com",
        ),
        UnopenableLinkModel(
            link = "https://[REDACTED_EMAIL]/page",
            expectedException = UntrustedDeeplinkException::class.java,
            scheme = "https",
            host = "tangem.com",
        ),
        UnopenableLinkModel(
            link = "http://tangem.com/page",
            expectedException = UnsupportedDeeplinkException::class.java,
            scheme = "http",
            host = "tangem.com",
        ),
        UnopenableLinkModel(
            link = "mailto:support@tangem.com",
            expectedException = UnsupportedDeeplinkException::class.java,
            scheme = "mailto",
            host = "",
        ),
    )

    private fun createUri(link: String): Uri {
        val uri = URI(link)

        return mockk {
            every { scheme } returns uri.scheme
            every { host } returns uri.host
            every { userInfo } returns uri.userInfo
        }
    }
}