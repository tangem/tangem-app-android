package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class ZeniqBlockchainMappingTest {

    @Test
    fun `GIVEN zeniq network id WHEN fromNetworkId THEN returns Zeniq`() {
        assertThat(Blockchain.fromNetworkId("zeniq")).isEqualTo(Blockchain.Zeniq)
    }

    @Test
    fun `GIVEN zeniq id WHEN fromId THEN returns Zeniq`() {
        assertThat(Blockchain.fromId("zeniq")).isEqualTo(Blockchain.Zeniq)
    }

    @Test
    fun `GIVEN Zeniq WHEN toNetworkId THEN returns zeniq`() {
        assertThat(Blockchain.Zeniq.toNetworkId()).isEqualTo("zeniq")
    }

    @Test
    fun `GIVEN Zeniq WHEN toCoinId THEN returns zeniq`() {
        assertThat(Blockchain.Zeniq.toCoinId()).isEqualTo("zeniq")
    }
}