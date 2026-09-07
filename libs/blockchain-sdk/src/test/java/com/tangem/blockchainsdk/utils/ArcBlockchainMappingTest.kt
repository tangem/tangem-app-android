package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class ArcBlockchainMappingTest {

    @Test
    fun `GIVEN arc network id WHEN fromNetworkId THEN returns Arc`() {
        assertThat(Blockchain.fromNetworkId("arc")).isEqualTo(Blockchain.Arc)
        assertThat(Blockchain.fromNetworkId("arc/test")).isEqualTo(Blockchain.ArcTestnet)
    }

    @Test
    fun `GIVEN arc id WHEN fromId THEN returns Arc`() {
        assertThat(Blockchain.fromId("arc")).isEqualTo(Blockchain.Arc)
        assertThat(Blockchain.fromId("arc/test")).isEqualTo(Blockchain.ArcTestnet)
    }

    @Test
    fun `GIVEN Arc WHEN toNetworkId THEN returns arc`() {
        assertThat(Blockchain.Arc.toNetworkId()).isEqualTo("arc")
        assertThat(Blockchain.ArcTestnet.toNetworkId()).isEqualTo("arc/test")
    }

    @Test
    fun `GIVEN Arc WHEN toCoinId THEN returns usd-coin`() {
        assertThat(Blockchain.Arc.toCoinId()).isEqualTo("usd-coin")
        assertThat(Blockchain.ArcTestnet.toCoinId()).isEqualTo("usd-coin")
    }
}