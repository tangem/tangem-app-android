package com.tangem.domain.collectibles.derivation

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import org.junit.jupiter.api.Test

internal class CollectiblesDerivationConstantsTest {

    @Test
    fun `GIVEN owner derivation path constant WHEN read THEN equals the expected path`() {
        // Assert
        assertThat(COLLECTIBLES_OWNER_DERIVATION_PATH).isEqualTo("m/44'/501'/999700'/0'/0'")
    }

    @Test
    fun `GIVEN owner derivation path constant WHEN nodes read THEN every node is hardened`() {
        // Act
        val nodes = COLLECTIBLES_OWNER_DERIVATION_PATH.removePrefix("m/").split("/")

        // Assert
        assertThat(nodes).isNotEmpty()
        assertThat(nodes.all { it.endsWith("'") }).isTrue()
    }
}