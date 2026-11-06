package com.tangem.domain.wallets.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SettingsTest {

    @Test
    fun `GIVEN connections button WHEN created THEN id and params match the spec`() {
        // Act
        val event = Settings.ButtonConnections()

        // Assert
        assertThat(event.id to event.params).isEqualTo("[Settings] Button - Connections" to emptyMap<String, String>())
    }
}