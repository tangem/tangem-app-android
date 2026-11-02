package com.tangem.blockchainsdk.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
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
                AnalyticsParam.BLOCKCHAIN_SELECTED_HOST to "https://next.host",
                AnalyticsParam.BLOCKCHAIN_EXCEPTION_HOST to "https://failed.host",
                AnalyticsParam.ERROR_DESCRIPTION to "timeout",
                AnalyticsParam.BLOCKCHAIN to "ethereum",
            ),
        )
    }
}