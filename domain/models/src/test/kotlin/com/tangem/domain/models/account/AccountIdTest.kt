package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountIdTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun forCryptoPortfolio(model: ForCryptoPortfolioModel) {
        // Arrange
        val userWalletId = UserWalletId("27163F47405CE73110837F24DF82607FF11C7AF9D78C93F409E4FEAFF3400C8F")

        // Act
        val actual = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = model.derivationIndex)

        // Assert
        Truth.assertThat(actual.value).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        ForCryptoPortfolioModel(
            derivationIndex = DerivationIndex.Main,
            expected = "4E39B13EA11E3B35339664A10BEF48F4AF752A1CC2200F79D23CB0FB3396C63F",
        ),
        ForCryptoPortfolioModel(
            derivationIndex = DerivationIndex(1).getOrNull()!!,
            expected = "7F22E71F8106783F0F2DAFCDE525E2F2A2281E864DDBE2FE668FA09329D563A2",
        ),
        ForCryptoPortfolioModel(
            derivationIndex = DerivationIndex(42).getOrNull()!!,
            expected = "555C1E17A302659446C97393453B7C2B3246AF4DA082C56C28FB6EDD1A6606A4",
        ),
    )

    data class ForCryptoPortfolioModel(
        val derivationIndex: DerivationIndex,
        val expected: String,
    )
}