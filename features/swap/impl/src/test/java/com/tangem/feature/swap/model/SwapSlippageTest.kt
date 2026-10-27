package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
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
            val actual = SwapSlippage.parseValid(
                value = model.input,
                provider = createProvider(slippage = BigDecimal("0.5")),
                defaultPercent = BigDecimal(model.defaultPercent),
            )

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
            ParseValidModel(input = "0.1", defaultPercent = "0.05", expected = "0.1"),
            ParseValidModel(input = "0.5", defaultPercent = "0.05", expected = null),
            ParseValidModel(input = "2", defaultPercent = "1", expected = "2"),
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsCustomAvailableFor {

        @ParameterizedTest
        @ProvideTestModels
        fun isCustomAvailableFor(model: IsCustomAvailableForModel) {
            // Act
            val actual = SwapSlippage.isCustomAvailableFor(model.toAccount)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<IsCustomAvailableForModel> {
            val userWalletId = UserWalletId("0011")
            return listOf(
                IsCustomAvailableForModel(
                    toAccount = Account.Personal.createMainAccount(userWalletId),
                    expected = true,
                ),
                IsCustomAvailableForModel(toAccount = Account.Payment(userWalletId), expected = false),
                IsCustomAvailableForModel(toAccount = Account.Money(userWalletId), expected = false),
                IsCustomAvailableForModel(toAccount = Account.Prediction(userWalletId), expected = false),
                IsCustomAvailableForModel(toAccount = Account.Collectibles(userWalletId), expected = false),
            )
        }
    }

    data class IsCustomAvailableForModel(val toAccount: Account, val expected: Boolean)

    data class PresetsModel(val default: String, val expected: List<String>)

    data class IsLowModel(val percent: String, val expected: Boolean)

    data class ParseValidModel(val input: String, val expected: String?, val defaultPercent: String = "0.5")

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