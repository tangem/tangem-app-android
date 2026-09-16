package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.*
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
        val expected: Result<Account.Personal>,
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
                    expected = createWalletAccountDTO(
                        userWalletId = userWallet.walletId,
                        type = WalletAccountDTO.Type.CRYPTO,
                    ),
                ),
            )
        }
    }

    data class ConvertBackModel(
        val value: Account.Personal,
        val expected: WalletAccountDTO,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class JointRow {

        private val token = UserTokensResponse.Token(
            networkId = "ethereum",
            derivationPath = "m/44'/60'/888888'/0/2",
            name = "Ethereum",
            symbol = "ETH",
            decimals = 18,
            contractAddress = null,
        )

        @Test
        fun `GIVEN joint row with tokens WHEN convert THEN currencies sit on the network at the owner key path`() {
            // Arrange
            val currency = MockCryptoCurrencyFactory().ethereum
            val ownerKeyPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")
            every {
                responseCryptoCurrenciesFactory.createCurrencies(
                    tokens = listOf(token),
                    userWallet = userWallet,
                    derivationPath = ownerKeyPath,
                )
            } returns listOf(currency)

            // Act
            val actual = converter.convert(value = jointRow(derivationIndex = 2, tokens = listOf(token)))

            // Assert
            Truth.assertThat(actual).isInstanceOf(Account.Joint::class.java)
            Truth.assertThat((actual as Account.Joint).ownerKeyIndex.value).isEqualTo(2)
            Truth.assertThat(actual.cryptoCurrencies).containsExactly(currency)
            verify(exactly = 0) {
                responseCryptoCurrenciesFactory.createCurrencies(
                    tokens = any(),
                    userWallet = any(),
                    accountIndex = any<DerivationIndex>(),
                )
            }
        }

        @Test
        fun `GIVEN joint row without tokens WHEN convert THEN account is empty`() {
            // Arrange
            every {
                responseCryptoCurrenciesFactory.createCurrencies(
                    tokens = emptyList(),
                    userWallet = userWallet,
                    derivationPath = any<Network.DerivationPath.Joint>(),
                )
            } returns emptyList()

            // Act
            val actual = converter.convert(value = jointRow(derivationIndex = 0, tokens = null))

            // Assert
            Truth.assertThat(actual).isInstanceOf(Account.Joint::class.java)
            Truth.assertThat(actual.cryptoCurrencies).isEmpty()
        }

        @Test
        fun `GIVEN joint account WHEN convertBack THEN row carries the joint type and the owner key index`() {
            // Arrange
            val account = Account.Joint(
                accountId = AccountId.forJointAccount(userWalletId = userWallet.walletId, value = JOINT_ID).getOrNull()!!,
                accountName = AccountName.Custom(value = "Family").getOrNull()!!,
                icon = CryptoPortfolioIcon.ofCustomAccount(
                    value = CryptoPortfolioIcon.Icon.Star,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
                ownerKeyIndex = OwnerKeyIndex(value = 2).getOrNull()!!,
                cryptoCurrencies = emptyList(),
            )

            // Act
            val actual = converter.convertBack(value = account)

            // Assert
            val expected = WalletAccountDTO(
                id = JOINT_ID,
                name = "Family",
                derivationIndex = 2,
                icon = CryptoPortfolioIcon.Icon.Star.name,
                iconColor = CryptoPortfolioIcon.Color.Azure.name,
                type = WalletAccountDTO.Type.JOINT,
                tokens = emptyList(),
            )
            Truth.assertThat(actual).isEqualTo(expected)
        }

        private fun jointRow(derivationIndex: Int, tokens: List<UserTokensResponse.Token>?): WalletAccountDTO {
            return createWalletAccountDTO(
                userWalletId = userWallet.walletId,
                accountId = JOINT_ID,
                accountName = "Family",
                derivationIndex = derivationIndex,
                tokens = tokens,
                type = WalletAccountDTO.Type.JOINT,
            )
        }
    }

    private companion object {
        const val JOINT_ID = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"
    }
}