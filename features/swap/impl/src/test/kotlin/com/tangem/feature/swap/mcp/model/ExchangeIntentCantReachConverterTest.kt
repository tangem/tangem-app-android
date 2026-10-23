package com.tangem.feature.swap.mcp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import org.junit.jupiter.api.Test

internal class ExchangeIntentCantReachConverterTest {

    private val converter = ExchangeIntentCantReachConverter()

    @Test
    fun `GIVEN no retry WHEN convert THEN the sheet is a warning closed by Got it`() {
        // Arrange
        var closed = false
        var retried = false

        // Act
        val actual = converter.convert(
            ExchangeIntentCantReachConverter.Input(onRetry = null, onClose = { closed = true }),
        )
        actual.primaryButton.onClick()

        // Assert
        assertThat(actual.variant).isEqualTo(TangemModalStatus.Variant.Warning)
        assertThat(actual.title).isEqualTo(stringReference("Can’t reach intent"))
        assertThat(actual.subtitle).isNull()
        assertThat(actual.primaryButton.text).isEqualTo(stringReference("Got it"))
        assertThat(closed).isTrue()
        assertThat(retried).isFalse()
    }

    @Test
    fun `GIVEN retry WHEN convert THEN the button tries again instead of closing`() {
        // Arrange
        var closed = false
        var retried = false

        // Act
        val actual = converter.convert(
            ExchangeIntentCantReachConverter.Input(onRetry = { retried = true }, onClose = { closed = true }),
        )
        actual.primaryButton.onClick()

        // Assert
        assertThat(actual.primaryButton.text).isEqualTo(stringReference("Try again"))
        assertThat(retried).isTrue()
        assertThat(closed).isFalse()
    }
}