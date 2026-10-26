package com.tangem.features.send.mcp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.features.send.mcp.createSendIntent
import com.tangem.features.send.mcp.createToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import java.math.BigDecimal
import java.util.Locale

@ResourceLock(Resources.LOCALE)
internal class SendIntentProcessedConverterTest {

    private val converter = SendIntentProcessedConverter(SendIntentAmountTextConverter())

    private val usdt = createToken()
    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUpLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN intent and currency WHEN convert THEN the subtitle names the send`() {
        // Act
        val actual = converter.convert(input(intent = createSendIntent(amount = BigDecimal("42"))))

        // Assert
        assertThat(actual.subtitle).isEqualTo(stringReference("Send 42.00 USDT"))
    }

    @Test
    fun `GIVEN no intent WHEN convert THEN there is no subtitle`() {
        // Act
        val actual = converter.convert(input(intent = null))

        // Assert
        assertThat(actual.subtitle).isNull()
    }

    @Test
    fun `GIVEN no currency WHEN convert THEN there is no subtitle`() {
        // Act
        val actual = converter.convert(
            SendIntentProcessedConverter.Input(intent = createSendIntent(), currency = null, onClose = {}),
        )

        // Assert
        assertThat(actual.subtitle).isNull()
    }

    @Test
    fun `GIVEN processed intent WHEN convert THEN the sheet is an info status closed by Got it`() {
        // Arrange
        var closed = false

        // Act
        val actual = converter.convert(input(onClose = { closed = true }))
        actual.primaryButton.onClick()

        // Assert
        assertThat(actual.variant).isEqualTo(TangemModalStatus.Variant.Info)
        assertThat(actual.title).isEqualTo(stringReference("This intent has already been processed"))
        assertThat(actual.primaryButton.text).isEqualTo(stringReference("Got it"))
        assertThat(closed).isTrue()
    }

    private fun input(intent: SendIntent? = createSendIntent(), onClose: () -> Unit = {}) =
        SendIntentProcessedConverter.Input(intent = intent, currency = usdt, onClose = onClose)
}