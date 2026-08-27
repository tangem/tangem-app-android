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
        val dw = deriver.deriveDepositWallet("0x1111111111111111111111111111111111111111")

        // Assert — the reference vector has upper-case hex letters, so a lowercased impl would fail this
        assertThat(dw).isNotEqualTo(dw.lowercase())
        assertThat(dw).isEqualTo("0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B")
    }

    internal data class Vector(val owner: String, val expectedDw: String)

    private fun provideTestModels() = listOf(
        Vector("0x1111111111111111111111111111111111111111", "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"),
        Vector("0x0000000000000000000000000000000000000001", "0x57ffBc34De23124fAeb8387fcd689d314E57aCcD"),
    )
}