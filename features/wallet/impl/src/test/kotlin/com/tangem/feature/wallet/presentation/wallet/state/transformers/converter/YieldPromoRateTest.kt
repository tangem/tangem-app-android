package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldPromoRateTest {

    private val factory = MockCryptoCurrencyFactory()
    private val usdcEvm = factory.createToken(
        blockchain = Blockchain.Ethereum,
        id = "usd-coin",
        contractAddress = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
    )
    private val usdcSolana = factory.createToken(
        blockchain = Blockchain.Solana,
        id = "usd-coin",
        contractAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
    )
    private val ethereum = factory.createCoin(Blockchain.Ethereum)
    private val percent = BigDecimal("5.5")

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a yield map WHEN yieldApyPercentOf THEN the match follows the address case rule of the network`(
        model: YieldRateModel,
    ) {
        // Act
        val actual = yieldApyPercentOf(model.currency, model.yieldMap)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    // yieldSupplyKey lowercases the contract, so a Solana key in the original case never matches
    private fun provideTestModels() = listOf(
        YieldRateModel(
            description = "EVM token AND upper-case key -> matched",
            currency = usdcEvm,
            yieldMap = mapOf("ETHEREUM_0XA0B86991C6218B36C1D19D4A2E9EB0CE3606EB48" to percent),
            expected = percent,
        ),
        YieldRateModel(
            description = "EVM token AND the key the token reports -> matched",
            currency = usdcEvm,
            yieldMap = mapOf(usdcEvm.yieldSupplyKey() to percent),
            expected = percent,
        ),
        YieldRateModel(
            description = "Solana token AND original-case key -> not matched",
            currency = usdcSolana,
            yieldMap = mapOf("solana_EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v" to percent),
            expected = null,
        ),
        YieldRateModel(
            description = "Solana token AND lower-case key -> matched",
            currency = usdcSolana,
            yieldMap = mapOf("solana_epjfwdd5aufqssqem2qn1xzybapc8g4weggkzwytdt1v" to percent),
            expected = percent,
        ),
        YieldRateModel(
            description = "coin AND a token in the map -> null",
            currency = ethereum,
            yieldMap = mapOf(usdcEvm.yieldSupplyKey() to percent),
            expected = null,
        ),
        YieldRateModel(
            description = "token missing in the map -> null",
            currency = usdcSolana,
            yieldMap = mapOf(usdcEvm.yieldSupplyKey() to percent),
            expected = null,
        ),
        YieldRateModel(
            description = "empty map -> null",
            currency = usdcEvm,
            yieldMap = emptyMap(),
            expected = null,
        ),
    )

    internal data class YieldRateModel(
        val description: String,
        val currency: CryptoCurrency,
        val yieldMap: Map<String, BigDecimal>,
        val expected: BigDecimal?,
    ) {
        override fun toString(): String = description
    }
}