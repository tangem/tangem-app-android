package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class KatanaBlockchainMappingTest {

    @Test
    fun `GIVEN katana network id WHEN fromNetworkId THEN returns Katana`() {
        assertThat(Blockchain.fromNetworkId("katana")).isEqualTo(Blockchain.Katana)
        assertThat(Blockchain.fromNetworkId("katana/test")).isEqualTo(Blockchain.KatanaTestnet)
    }

    @Test
    fun `GIVEN katana id WHEN fromId THEN returns Katana`() {
        assertThat(Blockchain.fromId("katana")).isEqualTo(Blockchain.Katana)
        assertThat(Blockchain.fromId("katana/test")).isEqualTo(Blockchain.KatanaTestnet)
    }

    @Test
    fun `GIVEN Katana WHEN toNetworkId THEN returns katana`() {
        assertThat(Blockchain.Katana.toNetworkId()).isEqualTo("katana")
        assertThat(Blockchain.KatanaTestnet.toNetworkId()).isEqualTo("katana/test")
    }

    @Test
    fun `GIVEN Katana WHEN toCoinId THEN returns katana-ethereum`() {
        assertThat(Blockchain.Katana.toCoinId()).isEqualTo("katana-ethereum")
        assertThat(Blockchain.KatanaTestnet.toCoinId()).isEqualTo("katana-ethereum")
    }
}