package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketChainFamilyTest {

    @ParameterizedTest
    @ProvideTestModels
    fun of(model: FamilyModel) {
        // Arrange
        val chainId = model.chainId

        // Act
        val actual = PolymarketChainFamily.of(chainId)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class FamilyModel(val chainId: String, val expected: PolymarketChainFamily?)

    private fun provideTestModels() = listOf(
        FamilyModel(chainId = "1", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "10", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "56", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "137", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "143", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "999", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "4663", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "8453", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "42161", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "57073", expected = PolymarketChainFamily.EVM),
        FamilyModel(chainId = "1151111081099710", expected = PolymarketChainFamily.SVM),
        FamilyModel(chainId = "728126428", expected = PolymarketChainFamily.TRON),
        FamilyModel(chainId = "8253038", expected = PolymarketChainFamily.BTC),
        FamilyModel(chainId = "324", expected = null),
        FamilyModel(chainId = "0x89", expected = null),
        FamilyModel(chainId = "", expected = null),
        FamilyModel(chainId = " 137", expected = null),
    )
}