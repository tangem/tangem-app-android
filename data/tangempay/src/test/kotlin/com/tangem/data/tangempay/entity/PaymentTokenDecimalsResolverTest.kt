package com.tangem.data.tangempay.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentTokenDecimalsResolverTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN launch network WHEN resolve decimals THEN 18 for BSC and 6 for the rest`(model: DecimalsModel) {
        // Act
        val actual = PaymentTokenDecimalsResolver.decimalsFor(model.blockchain)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        DecimalsModel(blockchain = Blockchain.BSC, expected = 18),
        DecimalsModel(blockchain = Blockchain.BSCTestnet, expected = 18),
        DecimalsModel(blockchain = Blockchain.Polygon, expected = 6),
        DecimalsModel(blockchain = Blockchain.Ethereum, expected = 6),
        DecimalsModel(blockchain = Blockchain.Base, expected = 6),
        DecimalsModel(blockchain = Blockchain.Arbitrum, expected = 6),
        DecimalsModel(blockchain = Blockchain.Tron, expected = 6),
    )

    internal data class DecimalsModel(val blockchain: Blockchain, val expected: Int)
}