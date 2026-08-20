package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class ElectroneumBlockchainMappingTest {

    @Test
    fun `GIVEN electroneum network id WHEN fromNetworkId THEN returns Electroneum`() {
        assertThat(Blockchain.fromNetworkId("electroneum")).isEqualTo(Blockchain.Electroneum)
        assertThat(Blockchain.fromNetworkId("electroneum/test")).isEqualTo(Blockchain.ElectroneumTestnet)
    }

    @Test
    fun `GIVEN electroneum id WHEN fromId THEN returns Electroneum`() {
        assertThat(Blockchain.fromId("electroneum")).isEqualTo(Blockchain.Electroneum)
        assertThat(Blockchain.fromId("electroneum/test")).isEqualTo(Blockchain.ElectroneumTestnet)
    }

    @Test
    fun `GIVEN Electroneum WHEN toNetworkId THEN returns electroneum`() {
        assertThat(Blockchain.Electroneum.toNetworkId()).isEqualTo("electroneum")
        assertThat(Blockchain.ElectroneumTestnet.toNetworkId()).isEqualTo("electroneum/test")
    }

    @Test
    fun `GIVEN Electroneum WHEN toCoinId THEN returns electroneum`() {
        assertThat(Blockchain.Electroneum.toCoinId()).isEqualTo("electroneum")
        assertThat(Blockchain.ElectroneumTestnet.toCoinId()).isEqualTo("electroneum")
    }
}