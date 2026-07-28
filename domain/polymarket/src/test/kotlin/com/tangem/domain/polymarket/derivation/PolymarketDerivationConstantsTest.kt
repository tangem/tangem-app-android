package com.tangem.domain.polymarket.derivation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class PolymarketDerivationConstantsTest {

    @Test
    fun `GIVEN owner derivation path constant WHEN read THEN equals the expected path`() {
        // Assert
        assertThat(POLYMARKET_OWNER_DERIVATION_PATH).isEqualTo("m/44'/60'/999997'/0/0")
    }
}