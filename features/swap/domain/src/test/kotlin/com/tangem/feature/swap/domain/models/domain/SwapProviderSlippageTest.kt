package com.tangem.feature.swap.domain.models.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.domain.buildSwapProvider
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SwapProviderSlippageTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DefaultSlippage {

        @ParameterizedTest
        @ProvideTestModels
        fun defaultSlippage(model: DefaultSlippageModel) {
            // Act
            val actual = buildSwapProvider(slippage = model.providerSlippage).defaultSlippage

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            DefaultSlippageModel(providerSlippage = BigDecimal("0.5"), expected = BigDecimal("0.5")),
            DefaultSlippageModel(providerSlippage = null, expected = null),
            DefaultSlippageModel(providerSlippage = BigDecimal.ZERO, expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PairDefaultSlippage {

        @ParameterizedTest
        @ProvideTestModels
        fun pairDefaultSlippage(model: PairDefaultSlippageModel) {
            // Arrange
            val provider = buildSwapProvider(providerId = PROVIDER_ID, slippage = model.providerSlippage)

            // Act
            val actual = provider.pairDefaultSlippage(model.quoteDefaults)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PairDefaultSlippageModel(
                providerSlippage = BigDecimal("2"),
                quoteDefaults = mapOf(PROVIDER_ID to BigDecimal("0.05")),
                expected = BigDecimal("0.05"),
            ),
            PairDefaultSlippageModel(
                providerSlippage = BigDecimal("2"),
                quoteDefaults = mapOf("other-provider" to BigDecimal("0.05")),
                expected = BigDecimal("2"),
            ),
            PairDefaultSlippageModel(
                providerSlippage = BigDecimal("2"),
                quoteDefaults = mapOf(PROVIDER_ID to BigDecimal.ZERO),
                expected = BigDecimal("2"),
            ),
            PairDefaultSlippageModel(
                providerSlippage = null,
                quoteDefaults = mapOf(PROVIDER_ID to BigDecimal("0.05")),
                expected = BigDecimal("0.05"),
            ),
            PairDefaultSlippageModel(providerSlippage = null, quoteDefaults = emptyMap(), expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AcceptsSlippage {

        @ParameterizedTest
        @ProvideTestModels
        fun acceptsSlippage(model: AcceptsSlippageModel) {
            // Arrange
            val provider = buildSwapProvider(
                isCustomSlippageSupported = model.isSupported,
                slippage = model.providerDefault,
            )

            // Act
            val actual = if (model.pairDefault != null) {
                provider.acceptsSlippage(BigDecimal(model.percent), defaultPercent = model.pairDefault)
            } else {
                provider.acceptsSlippage(BigDecimal(model.percent))
            }

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AcceptsSlippageModel(isSupported = true, providerDefault = BigDecimal("0.5"), percent = "0.7", expected = true),
            AcceptsSlippageModel(isSupported = true, providerDefault = BigDecimal("0.5"), percent = "1", expected = true),
            AcceptsSlippageModel(isSupported = true, providerDefault = BigDecimal("0.5"), percent = "0.01", expected = true),
            AcceptsSlippageModel(isSupported = true, providerDefault = BigDecimal("0.5"), percent = "1.01", expected = false),
            AcceptsSlippageModel(isSupported = true, providerDefault = BigDecimal("0.5"), percent = "0.009", expected = false),
            AcceptsSlippageModel(isSupported = false, providerDefault = BigDecimal("0.5"), percent = "0.7", expected = false),
            AcceptsSlippageModel(isSupported = true, providerDefault = null, percent = "0.7", expected = false),
            AcceptsSlippageModel(isSupported = true, providerDefault = BigDecimal.ZERO, percent = "0.7", expected = false),
            AcceptsSlippageModel(
                isSupported = true,
                providerDefault = BigDecimal("2"),
                pairDefault = BigDecimal("0.05"),
                percent = "0.1",
                expected = true,
            ),
            AcceptsSlippageModel(
                isSupported = true,
                providerDefault = BigDecimal("2"),
                pairDefault = BigDecimal("0.05"),
                percent = "0.11",
                expected = false,
            ),
            AcceptsSlippageModel(
                isSupported = true,
                providerDefault = BigDecimal("0.5"),
                pairDefault = BigDecimal("1"),
                percent = "2",
                expected = true,
            ),
            AcceptsSlippageModel(
                isSupported = false,
                providerDefault = BigDecimal("2"),
                pairDefault = BigDecimal("0.05"),
                percent = "0.1",
                expected = false,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AcceptedSlippageOrNull {

        @ParameterizedTest
        @ProvideTestModels
        fun acceptedSlippageOrNull(model: AcceptedSlippageOrNullModel) {
            // Arrange
            val provider = buildSwapProvider(
                isCustomSlippageSupported = model.isSupported,
                slippage = BigDecimal("0.5"),
            )

            // Act
            val actual = provider.acceptedSlippageOrNull(model.custom)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AcceptedSlippageOrNullModel(isSupported = true, custom = BigDecimal("0.8"), expected = BigDecimal("0.8")),
            AcceptedSlippageOrNullModel(isSupported = false, custom = BigDecimal("0.8"), expected = null),
            AcceptedSlippageOrNullModel(isSupported = true, custom = BigDecimal("1.5"), expected = null),
            AcceptedSlippageOrNullModel(isSupported = true, custom = null, expected = null),
        )
    }

    data class DefaultSlippageModel(val providerSlippage: BigDecimal?, val expected: BigDecimal?)

    data class PairDefaultSlippageModel(
        val providerSlippage: BigDecimal?,
        val quoteDefaults: Map<String, BigDecimal>,
        val expected: BigDecimal?,
    )

    data class AcceptsSlippageModel(
        val isSupported: Boolean,
        val providerDefault: BigDecimal?,
        val percent: String,
        val expected: Boolean,
        val pairDefault: BigDecimal? = null,
    )

    data class AcceptedSlippageOrNullModel(val isSupported: Boolean, val custom: BigDecimal?, val expected: BigDecimal?)

    private companion object {
        const val PROVIDER_ID = "provider"
    }
}