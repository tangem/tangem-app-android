package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoPortfolioConverterTest {

    private val userWallet = mockk<UserWallet> {
        every { walletId } returns UserWalletId("011")
    }

    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory = mockk()
    private val userTokensResponseFactory: UserTokensResponseFactory = mockk()
    private val converter = CryptoPortfolioConverter(
        userWallet = userWallet,
        responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        userTokensResponseFactory = userTokensResponseFactory,
    )

    @BeforeEach
    fun setupEach() {
        clearMocks(responseCryptoCurrenciesFactory, userTokensResponseFactory)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ConvertModel) {
            // Act
            val actual = runCatching { converter.convert(model.value) }

            // Asset
            actual
                .onSuccess {
                    val expected = model.expected.getOrNull()
                    Truth.assertThat(it).isEqualTo(expected)
                }
                .onFailure {
                    val expected = model.expected.exceptionOrNull() ?: throw it
                    Truth.assertThat(it).isInstanceOf(expected::class.java)
                    Truth.assertThat(it.message).isEqualTo(expected.message)
                }
        }

        private fun provideTestModels(): List<ConvertModel> {
            return listOf(
                ConvertModel(
                    value = createWalletAccountDTO(userWalletId = userWallet.walletId),
                    expected = Result.success(createCryptoPortfolio(userWalletId = userWallet.walletId)),
                ),
                ConvertModel(
                    value = createWalletAccountDTO(userWalletId = userWallet.walletId, accountId = "123"),
                    expected = Result.failure(
                        IllegalStateException(
                            "Unable to create AccountId from value: 123. Cause: ${AccountId.Error.InvalidFormat}",
                        ),
                    ),
                ),
                ConvertModel(
                    value = createWalletAccountDTO(userWalletId = userWallet.walletId, accountName = null),
                    expected = Result.success(
                        createCryptoPortfolio(userWalletId = userWallet.walletId).copy(
                            accountName = AccountName.DefaultMain,
                        ),
                    ),
                ),
                ConvertModel(
                    value = createWalletAccountDTO(userWalletId = userWallet.walletId, accountName = ""),
                    expected = Result.failure(
                        IllegalStateException(
                            "Unable to create AccountName from value: . Cause: ${AccountName.Error.Empty}",
                        ),
                    ),
                ),
                ConvertModel(
                    value = createWalletAccountDTO(userWalletId = userWallet.walletId, icon = "INVALID_ICON"),
                    expected = Result.failure(
                        IllegalArgumentException(
                            "No enum constant com.tangem.domain.models.account.CryptoPortfolioIcon.Icon.INVALID_ICON",
                        ),
                    ),
                ),
                ConvertModel(
                    value = createWalletAccountDTO(userWalletId = userWallet.walletId, iconColor = "INVALID_COLOR"),
                    expected = Result.failure(
                        IllegalArgumentException(
                            "No enum constant com.tangem.domain.models.account.CryptoPortfolioIcon.Color.INVALID_COLOR",
                        ),
                    ),
                ),
                ConvertModel(
                    value = createWalletAccountDTO(userWalletId = userWallet.walletId, derivationIndex = -1),
                    expected = Result.failure(
                        IllegalStateException(
                            "Unable to create DerivationIndex from value: -1. " +
                                "Cause: NegativeDerivationIndex: Derivation index cannot be negative: -1",
                        ),
                    ),
                ),
                ConvertModel(
                    value = createWalletAccountDTO(userWalletId = userWallet.walletId, tokens = null),
                    expected = Result.failure(
                        IllegalStateException("Tokens should not be null"),
                    ),
                ),
            )
        }
    }

    data class ConvertModel(
        val value: WalletAccountDTO,
        val expected: Result<Account.CryptoPortfolio>,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertBack {

        @ParameterizedTest
        @ProvideTestModels
        fun convertBack(model: ConvertBackModel) {
            // Act
            val actual = converter.convertBack(model.value)

            // Assert
            val expected = model.expected
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels(): List<ConvertBackModel> {
            return listOf(
                ConvertBackModel(
                    value = createCryptoPortfolio(userWalletId = userWallet.walletId),
                    expected = createWalletAccountDTO(userWalletId = userWallet.walletId),
                ),
            )
        }
    }

    data class ConvertBackModel(
        val value: Account.CryptoPortfolio,
        val expected: WalletAccountDTO,
    )
}