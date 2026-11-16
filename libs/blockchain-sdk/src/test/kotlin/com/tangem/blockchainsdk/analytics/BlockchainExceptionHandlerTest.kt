package com.tangem.blockchainsdk.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BlockchainExceptionHandlerTest {

    private val analyticsErrorHandler: AnalyticsErrorHandler = mockk(relaxed = true)
    private val handler = BlockchainExceptionHandler(analyticsErrorHandler)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsErrorHandler)
    }

    @Test
    fun `GIVEN api switch WHEN handleApiSwitch THEN sends blockchain exception event`() {
        // Arrange
        val event = slot<AnalyticsEvent>()

        // Act
        handler.handleApiSwitch(
            currentHost = "https://failed.host",
            nextHost = "https://next.host",
            message = "timeout",
            blockchain = Blockchain.Ethereum,
        )

        // Assert
        verify(exactly = 1) { analyticsErrorHandler.sendErrorEvent(capture(event)) }
        assertThat(event.captured.id).isEqualTo("[BlockchainSdk] Exception")
        assertThat(event.captured.params).isEqualTo(
            mapOf(
                "selected_host" to "https://next.host",
                "exception_host" to "https://failed.host",
                "Error Description" to "timeout",
                "Blockchain" to "ethereum",
            ),
        )
    }
}