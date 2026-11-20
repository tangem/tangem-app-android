package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class XLayerBlockchainMappingTest {

    @Test
    fun `GIVEN x-layer network id WHEN fromNetworkId THEN returns XLayer`() {
        assertThat(Blockchain.fromNetworkId("x-layer")).isEqualTo(Blockchain.XLayer)
        assertThat(Blockchain.fromNetworkId("x-layer/test")).isEqualTo(Blockchain.XLayerTestnet)
    }

    @Test
    fun `GIVEN x-layer id WHEN fromId THEN returns XLayer`() {
        assertThat(Blockchain.fromId("x-layer")).isEqualTo(Blockchain.XLayer)
        assertThat(Blockchain.fromId("x-layer/test")).isEqualTo(Blockchain.XLayerTestnet)
    }

    @Test
    fun `GIVEN XLayer WHEN toNetworkId THEN returns x-layer`() {
        assertThat(Blockchain.XLayer.toNetworkId()).isEqualTo("x-layer")
        assertThat(Blockchain.XLayerTestnet.toNetworkId()).isEqualTo("x-layer/test")
    }

    @Test
    fun `GIVEN XLayer WHEN toCoinId THEN returns okb`() {
        assertThat(Blockchain.XLayer.toCoinId()).isEqualTo("okb")
        assertThat(Blockchain.XLayerTestnet.toCoinId()).isEqualTo("okb")
    }
}