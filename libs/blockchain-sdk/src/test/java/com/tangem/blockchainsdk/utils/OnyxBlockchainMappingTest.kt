package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class OnyxBlockchainMappingTest {

    @Test
    fun `GIVEN onyx network id WHEN fromNetworkId THEN returns Onyx`() {
        assertThat(Blockchain.fromNetworkId("onyx")).isEqualTo(Blockchain.Onyx)
        assertThat(Blockchain.fromNetworkId("onyx/test")).isEqualTo(Blockchain.OnyxTestnet)
    }

    @Test
    fun `GIVEN onyx id WHEN fromId THEN returns Onyx`() {
        assertThat(Blockchain.fromId("onyx")).isEqualTo(Blockchain.Onyx)
        assertThat(Blockchain.fromId("onyx/test")).isEqualTo(Blockchain.OnyxTestnet)
    }

    @Test
    fun `GIVEN Onyx WHEN toNetworkId THEN returns onyx`() {
        assertThat(Blockchain.Onyx.toNetworkId()).isEqualTo("onyx")
        assertThat(Blockchain.OnyxTestnet.toNetworkId()).isEqualTo("onyx/test")
    }

    @Test
    fun `GIVEN Onyx WHEN toCoinId THEN returns chain-2`() {
        assertThat(Blockchain.Onyx.toCoinId()).isEqualTo("chain-2")
        assertThat(Blockchain.OnyxTestnet.toCoinId()).isEqualTo("chain-2")
    }
}