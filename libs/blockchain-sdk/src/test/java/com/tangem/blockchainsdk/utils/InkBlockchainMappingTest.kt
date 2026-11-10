package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class InkBlockchainMappingTest {

    @Test
    fun `GIVEN ink network id WHEN fromNetworkId THEN returns Ink`() {
        assertThat(Blockchain.fromNetworkId("ink")).isEqualTo(Blockchain.Ink)
        assertThat(Blockchain.fromNetworkId("ink/test")).isEqualTo(Blockchain.InkTestnet)
    }

    @Test
    fun `GIVEN ink id WHEN fromId THEN returns Ink`() {
        assertThat(Blockchain.fromId("ink")).isEqualTo(Blockchain.Ink)
        assertThat(Blockchain.fromId("ink/test")).isEqualTo(Blockchain.InkTestnet)
    }

    @Test
    fun `GIVEN Ink WHEN toNetworkId THEN returns ink`() {
        assertThat(Blockchain.Ink.toNetworkId()).isEqualTo("ink")
        assertThat(Blockchain.InkTestnet.toNetworkId()).isEqualTo("ink/test")
    }

    @Test
    fun `GIVEN Ink WHEN toCoinId THEN returns ink-ethereum`() {
        assertThat(Blockchain.Ink.toCoinId()).isEqualTo("ink-ethereum")
        assertThat(Blockchain.InkTestnet.toCoinId()).isEqualTo("ink-ethereum")
    }
}