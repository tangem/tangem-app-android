package com.tangem.domain.tokens.model.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenReceiveNewAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN event WHEN created THEN id and params match the spec`(model: EventModel) {
        // Act
        val actual = model.event.id to model.event.params

        // Assert
        assertThat(actual).isEqualTo(model.expectedId to model.expectedParams)
    }

    private fun provideTestModels() = listOf(
        EventModel(
            event = TokenReceiveNewAnalyticsEvent.ReceiveScreenOpened(
                token = "ETH",
                blockchainName = "Ethereum",
                ensStatus = AnalyticsParam.EmptyFull.Full,
            ),
            expectedId = "[Token / Receive] Receive Screen Opened",
            expectedParams = mapOf("Token" to "ETH", "Blockchain" to "Ethereum", "ENS" to "Full"),
        ),
        EventModel(
            event = TokenReceiveNewAnalyticsEvent.ReceiveScreenOpened(
                token = "ETH",
                blockchainName = "Ethereum",
                ensStatus = AnalyticsParam.EmptyFull.Empty,
            ),
            expectedId = "[Token / Receive] Receive Screen Opened",
            expectedParams = mapOf("Token" to "ETH", "Blockchain" to "Ethereum", "ENS" to "Empty"),
        ),
        copyAddress(source = TokenReceiveCopyActionSource.Main, expectedSource = "Main"),
        copyAddress(source = TokenReceiveCopyActionSource.Token, expectedSource = "Token"),
        copyAddress(source = TokenReceiveCopyActionSource.Receive, expectedSource = "Receive"),
        copyAddress(source = TokenReceiveCopyActionSource.QR, expectedSource = "QR"),
        copyAddress(source = TokenReceiveCopyActionSource.CopyButton, expectedSource = "Copy button"),
        copyAddress(source = TokenReceiveCopyActionSource.QrCode, expectedSource = "QR code"),
        EventModel(
            event = TokenReceiveNewAnalyticsEvent.ButtonShareAddress(
                token = "BTC",
                blockchainName = "Bitcoin",
                source = TokenReceiveCopyActionSource.Receive,
            ),
            expectedId = "[Token / Receive] Button - Share Address",
            expectedParams = mapOf("Token" to "BTC", "Blockchain" to "Bitcoin", "Source" to "Receive"),
        ),
        EventModel(
            event = TokenReceiveNewAnalyticsEvent.ButtonCopyEns(token = "ETH", blockchainName = "Ethereum"),
            expectedId = "[Token / Receive] Button - ENS",
            expectedParams = mapOf("Token" to "ETH", "Blockchain" to "Ethereum"),
        ),
    )

    private fun copyAddress(source: TokenReceiveCopyActionSource, expectedSource: String) = EventModel(
        event = TokenReceiveNewAnalyticsEvent.ButtonCopyAddress(
            token = "BTC",
            blockchainName = "Bitcoin",
            tokenReceiveSource = source,
        ),
        expectedId = "[Token / Receive] Button - Copy Address",
        expectedParams = mapOf("Token" to "BTC", "Blockchain" to "Bitcoin", "Source" to expectedSource),
    )

    data class EventModel(
        val event: AnalyticsEvent,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    )
}