package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.feature.wallet.impl.R
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldApyPercentOf {

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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToYieldPromoText {

        private var previousLocale: Locale = Locale.getDefault()

        @BeforeEach
        fun pinLocale() {
            previousLocale = Locale.getDefault()
            Locale.setDefault(Locale.US)
        }

        @AfterEach
        fun restoreLocale() {
            Locale.setDefault(previousLocale)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a backend percent WHEN toYieldPromoText THEN the text shows it as a rate with two decimals`(
            model: PromoTextModel,
        ) {
            // Arrange
            val expected = resourceReference(
                R.string.yield_module_main_screen_promo_banner_message,
                wrappedList(model.expectedRate),
            )

            // Act
            val actual = model.percent.toYieldPromoText()

            // Assert
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            PromoTextModel(
                description = "5.5 percent -> 5.50",
                percent = BigDecimal("5.5"),
                expectedRate = "5.50",
            ),
            PromoTextModel(
                description = "12 percent -> 12.00",
                percent = BigDecimal("12"),
                expectedRate = "12.00",
            ),
            PromoTextModel(
                description = "below 0.01 percent -> <0.01",
                percent = BigDecimal("0.001"),
                expectedRate = "<0.01",
            ),
        )
    }

    internal data class YieldRateModel(
        val description: String,
        val currency: CryptoCurrency,
        val yieldMap: Map<String, BigDecimal>,
        val expected: BigDecimal?,
    ) {
        override fun toString(): String = description
    }

    internal data class PromoTextModel(
        val description: String,
        val percent: BigDecimal,
        val expectedRate: String,
    ) {
        override fun toString(): String = description
    }
}