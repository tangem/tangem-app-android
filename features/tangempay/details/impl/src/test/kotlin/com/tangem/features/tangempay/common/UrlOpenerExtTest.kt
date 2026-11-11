package com.tangem.features.tangempay.common

import com.tangem.core.navigation.url.UrlOpener
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UrlOpenerExtTest {

    private val urlOpener: UrlOpener = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(urlOpener)
    }

    @Test
    fun `GIVEN trusted link with surrounding whitespace WHEN openTrustedLink THEN trimmed link is opened in app`() {
        // Act
        urlOpener.openTrustedLink("  https://tangem.com/docs/en/tangem-pay-cashback-terms.pdf\n")

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/docs/en/tangem-pay-cashback-terms.pdf") }
    }

    @Test
    fun `GIVEN app deep link WHEN openTrustedLink THEN nothing is opened`() {
        // Act
        urlOpener.openTrustedLink("tangem://wc?uri=wc:topic@2?relay-protocol=irn&symKey=secret")

        // Assert
        verify(exactly = 0) { urlOpener.openUrl(any()) }
        verify(exactly = 0) { urlOpener.openUrlExternalBrowser(any()) }
    }
}