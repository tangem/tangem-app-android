package com.tangem.data.polymarket.derivation

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPolymarketDepositWalletDeriverTest {

    private val deriver = DefaultPolymarketDepositWalletDeriver()

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN owner EOA WHEN deriveDepositWallet THEN returns byte-exact ERC-55 DW`(model: Vector) {
        // Act
        val dw = deriver.deriveDepositWallet(model.owner)

        // Assert
        assertThat(dw).isEqualTo(model.expectedDw)
    }

    @Test
    fun `GIVEN owner EOA WHEN deriveDepositWallet THEN output is ERC-55 checksummed not lowercase`() {
        // Act
        val dw = deriver.deriveDepositWallet("0x0491eb219E3D2d05aEF0C35D1079c0a55b19bd2B")

        // Assert — the reference vector has upper-case hex letters, so a lowercased impl would fail this
        assertThat(dw).isNotEqualTo(dw.lowercase())
        assertThat(dw).isEqualTo("0xdf1a31b50D3F99d4460ACC1Bc99aB2e09BCcC538")
    }

    internal data class Vector(val owner: String, val expectedDw: String)

    /**
     * Real owner→DW pairs read off deployments the factory made on Polygon, so a recipe that only agrees
     * with itself cannot pass. Recover more with `eth_getLogs` on [PolymarketContracts.DW_FACTORY]: an event's
     * first indexed topic is the deployed wallet, its second the owner.
     */
    private fun provideTestModels() = listOf(
        Vector("0x0491eb219E3D2d05aEF0C35D1079c0a55b19bd2B", "0xdf1a31b50D3F99d4460ACC1Bc99aB2e09BCcC538"),
        Vector("0xd22b712FAA5f28ebCc3D75aE5356ce8ea2D18F71", "0x5B69409F9bF5034107D361ab3Ac944CF3C74f7D3"),
        Vector("0x47b7eBE053a75d81cb6F2CC815d077e5057345e5", "0xD6aCb4e9B654e2fD440f490977ED9b686D5D86eb"),
    )
}