package com.tangem.data.pay.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class PaymentTokenDecimalsResolverTest {

    @Test
    fun `GIVEN BSC WHEN resolve decimals THEN 18`() {
        assertThat(PaymentTokenDecimalsResolver.decimalsFor(Blockchain.BSC)).isEqualTo(18)
    }

    @Test
    fun `GIVEN BSC testnet WHEN resolve decimals THEN 18`() {
        assertThat(PaymentTokenDecimalsResolver.decimalsFor(Blockchain.BSCTestnet)).isEqualTo(18)
    }

    @Test
    fun `GIVEN non-BSC launch networks WHEN resolve decimals THEN 6`() {
        val launchNetworks = listOf(
            Blockchain.Polygon,
            Blockchain.Ethereum,
            Blockchain.Base,
            Blockchain.Arbitrum,
            Blockchain.Tron,
        )

        launchNetworks.forEach { blockchain ->
            assertThat(PaymentTokenDecimalsResolver.decimalsFor(blockchain)).isEqualTo(6)
        }
    }
}