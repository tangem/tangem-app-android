package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class IgraBlockchainMappingTest {

    @Test
    fun `GIVEN igra network id WHEN fromNetworkId THEN returns Igra`() {
        assertThat(Blockchain.fromNetworkId("igra")).isEqualTo(Blockchain.Igra)
        assertThat(Blockchain.fromNetworkId("igra/test")).isEqualTo(Blockchain.IgraTestnet)
    }

    @Test
    fun `GIVEN igra id WHEN fromId THEN returns Igra`() {
        assertThat(Blockchain.fromId("igra")).isEqualTo(Blockchain.Igra)
        assertThat(Blockchain.fromId("igra/test")).isEqualTo(Blockchain.IgraTestnet)
    }

    @Test
    fun `GIVEN Igra WHEN toNetworkId THEN returns igra`() {
        assertThat(Blockchain.Igra.toNetworkId()).isEqualTo("igra")
        assertThat(Blockchain.IgraTestnet.toNetworkId()).isEqualTo("igra/test")
    }

    @Test
    fun `GIVEN Igra WHEN toCoinId THEN returns igra-bridged-kaspa`() {
        assertThat(Blockchain.Igra.toCoinId()).isEqualTo("igra-bridged-kaspa")
        assertThat(Blockchain.IgraTestnet.toCoinId()).isEqualTo("igra-bridged-kaspa")
    }
}