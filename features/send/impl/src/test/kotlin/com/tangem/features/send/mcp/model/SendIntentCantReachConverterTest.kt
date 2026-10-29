package com.tangem.features.send.mcp.model

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.send.impl.R
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import org.junit.jupiter.api.Test

internal class SendIntentCantReachConverterTest {

    private val converter = SendIntentCantReachConverter()

    @Test
    fun `GIVEN no retry WHEN convert THEN the sheet is a warning closed by Got it`() {
        // Arrange
        var closed = false

        // Act
        val actual = converter.convert(SendIntentCantReachConverter.Input(onRetry = null, onClose = { closed = true }))
        actual.primaryButton.onClick()

        // Assert
        assertThat(actual.variant).isEqualTo(TangemModalStatus.Variant.Warning)
        assertThat(actual.title).isEqualTo(resourceReference(R.string.mcp_send_intent_cant_reach_title))
        assertThat(actual.subtitle).isNull()
        assertThat(actual.primaryButton.text).isEqualTo(resourceReference(R.string.common_got_it))
        assertThat(closed).isTrue()
    }

    @Test
    fun `GIVEN retry WHEN convert THEN the button tries again instead of closing`() {
        // Arrange
        var closed = false
        var retried = false

        // Act
        val actual = converter.convert(
            SendIntentCantReachConverter.Input(onRetry = { retried = true }, onClose = { closed = true }),
        )
        actual.primaryButton.onClick()

        // Assert
        assertThat(actual.primaryButton.text).isEqualTo(resourceReference(R.string.alert_button_try_again))
        assertThat(retried).isTrue()
        assertThat(closed).isFalse()
    }
}