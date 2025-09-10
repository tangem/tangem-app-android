package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchivedAccountConverterTest {

    private val userWalletId = UserWalletId("011")
    private val converter = ArchivedAccountConverter(userWalletId = userWalletId)

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: TestModel) {
        // Act
        val actual = runCatching { converter.convert(value = model.value) }

        // Assert
        actual
            .onSuccess {
                val expected = model.expected.getOrNull()!!
                Truth.assertThat(it).isEqualTo(expected)
            }
            .onFailure {
                val expected = model.expected.exceptionOrNull()!!
                Truth.assertThat(it).isInstanceOf(expected::class.java)
                Truth.assertThat(it.message).isEqualTo(expected.message)
            }
    }

    private fun provideTestModels(): List<TestModel> {
        return listOf(
            TestModel(
                value = createDTO(),
                expected = Result.success(createDomain()),
            ),
            TestModel(
                value = createDTO(accountId = "123"),
                expected = Result.failure(
                    IllegalStateException(
                        "Unable to create AccountId from value: 123. Cause: ${AccountId.Error.InvalidFormat}",
                    ),
                ),
            ),
            TestModel(
                value = createDTO(name = null),
                expected = Result.success(
                    createDomain().copy(name = AccountName.DefaultMain),
                ),
            ),
            TestModel(
                value = createDTO(name = ""),
                expected = Result.failure(
                    IllegalStateException(
                        "Unable to create AccountName from value: . Cause: ${AccountName.Error.Empty}",
                    ),
                ),
            ),
            TestModel(
                value = createDTO(icon = "INVALID_ICON"),
                expected = Result.failure(
                    IllegalArgumentException(
                        "No enum constant com.tangem.domain.models.account.CryptoPortfolioIcon.Icon.INVALID_ICON",
                    ),
                ),
            ),
            TestModel(
                value = createDTO(iconColor = "INVALID_COLOR"),
                expected = Result.failure(
                    IllegalArgumentException(
                        "No enum constant com.tangem.domain.models.account.CryptoPortfolioIcon.Color.INVALID_COLOR",
                    ),
                ),
            ),
            TestModel(
                value = createDTO(derivationIndex = -1),
                expected = Result.failure(
                    IllegalStateException(
                        "Unable to create DerivationIndex from value: -1. " +
                            "Cause: NegativeDerivationIndex: Derivation index cannot be negative: -1",
                    ),
                ),
            ),
            TestModel(
                value = createDTO(totalTokens = null),
                expected = Result.failure(
                    IllegalStateException("Total tokens should not be null"),
                ),
            ),
            TestModel(
                value = createDTO(totalNetworks = null),
                expected = Result.failure(
                    IllegalStateException("Total networks should not be null"),
                ),
            ),
        )
    }

    private fun createDTO(
        accountId: String = "957B88B12730E646E0F33D3618B77DFA579E8231E3C59C7104BE7165611C8027",
        name: String? = "Test Account",
        icon: String = "Letter",
        iconColor: String = "Azure",
        derivationIndex: Int = 0,
        totalTokens: Int? = 1,
        totalNetworks: Int? = 1,
    ): WalletAccountDTO {
        return WalletAccountDTO(
            id = accountId,
            name = name,
            derivationIndex = derivationIndex,
            icon = icon,
            iconColor = iconColor,
            tokens = null,
            totalTokens = totalTokens,
            totalNetworks = totalNetworks,
        )
    }

    private fun createDomain(): ArchivedAccount {
        return ArchivedAccount(
            accountId = AccountId.forCryptoPortfolio(userWalletId, DerivationIndex(0).getOrNull()!!),
            name = AccountName("Test Account").getOrNull()!!,
            derivationIndex = 0.toDerivationIndex(),
            icon = CryptoPortfolioIcon.ofCustomAccount(
                value = CryptoPortfolioIcon.Icon.Letter,
                color = CryptoPortfolioIcon.Color.Azure,
            ),
            tokensCount = 1,
            networksCount = 1,
        )
    }

    data class TestModel(
        val value: WalletAccountDTO,
        val expected: Result<ArchivedAccount>,
    )
}