package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SwapSlippageTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Presets {

        @ParameterizedTest
        @ProvideTestModels
        fun presets(model: PresetsModel) {
            // Act
            val actual = SwapSlippage.presets(BigDecimal(model.default)).map(BigDecimal::toPlainString)

            // Assert
            assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
        }

        private fun provideTestModels() = listOf(
            PresetsModel(default = "0.3", expected = listOf("0.15", "0.3", "0.6")),
            PresetsModel(default = "1", expected = listOf("0.5", "1", "2")),
            PresetsModel(default = "0.01", expected = listOf("0.01", "0.02")),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ParseValid {

        @ParameterizedTest
        @ProvideTestModels
        fun parseValid(model: ParseValidModel) {
            // Act
            val actual = SwapSlippage.parseValid(model.input, createProvider(slippage = BigDecimal("0.5")))

            // Assert
            assertThat(actual?.toPlainString()).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ParseValidModel(input = "0.01", expected = "0.01"),
            ParseValidModel(input = "0.5", expected = "0.5"),
            ParseValidModel(input = "1", expected = "1"),
            ParseValidModel(input = "1.01", expected = null),
            ParseValidModel(input = "0.009", expected = null),
            ParseValidModel(input = "0", expected = null),
            ParseValidModel(input = "", expected = null),
            ParseValidModel(input = "abc", expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsLow {

        @ParameterizedTest
        @ProvideTestModels
        fun isLow(model: IsLowModel) {
            // Act
            val actual = SwapSlippage.isLow(BigDecimal(model.percent), defaultPercent = BigDecimal("0.3"))

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            IsLowModel(percent = "0.01", expected = true),
            IsLowModel(percent = "0.11", expected = true),
            IsLowModel(percent = "0.149", expected = true),
            IsLowModel(percent = "0.15", expected = false),
            IsLowModel(percent = "0.3", expected = false),
            IsLowModel(percent = "0.6", expected = false),
        )
    }

    data class PresetsModel(val default: String, val expected: List<String>)

    data class IsLowModel(val percent: String, val expected: Boolean)

    data class ParseValidModel(val input: String, val expected: String?)

    private fun createProvider(slippage: BigDecimal) = SwapProvider(
        providerId = "1inch",
        name = "1inch",
        type = ExchangeProviderType.DEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = slippage,
        isCustomSlippageSupported = true,
    )
}