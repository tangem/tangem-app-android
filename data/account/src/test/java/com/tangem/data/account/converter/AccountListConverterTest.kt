package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountListConverterTest {

    private val userWallet = mockk<UserWallet> {
        every { walletId } returns UserWalletId("011")
    }
    private val cryptoPortfolioConverterFactory = mockk<CryptoPortfolioConverter.Factory>()
    private val cryptoPortfolioConverter = mockk<CryptoPortfolioConverter>()
    private val converter = AccountListConverter(userWallet, cryptoPortfolioConverterFactory)

    @BeforeAll
    fun setupAll() {
        every { cryptoPortfolioConverterFactory.create(userWallet) } returns cryptoPortfolioConverter
    }

    @BeforeEach
    fun setupEach() {
        clearMocks(cryptoPortfolioConverter)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @Test
        fun `cryptoPortfolioConverter throws exception`() {
            // Arrange
            val dto = createGetWalletAccountsResponse(userWallet.walletId)
            val exception = IllegalStateException("Test exception")

            every { cryptoPortfolioConverter.convert(any()) } throws exception

            // Act
            val actual = runCatching { converter.convert(dto) }.exceptionOrNull()!!

            // Asset
            val expected = exception
            Truth.assertThat(actual).isInstanceOf(expected::class.java)
            Truth.assertThat(actual.message).isEqualTo(expected.message)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun convert(model: ConvertModel) {
            // Arrange
            if (model.expected.isSuccess) {
                model.value.accounts.forEach { dto ->
                    val account = model.expected.getOrNull()!!.accounts
                        .firstOrNull { it.accountId.value == dto.id } as? Account.CryptoPortfolio

                    every { cryptoPortfolioConverter.convert(dto) } returns account!!
                }
            }

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
                    value = createGetWalletAccountsResponse(
                        userWalletId = userWallet.walletId,
                        sortType = UserTokensResponse.SortType.BALANCE,
                        groupType = UserTokensResponse.GroupType.NETWORK,
                    ),
                    expected = Result.success(
                        createAccountList(
                            userWallet = userWallet,
                            sortType = TokensSortType.BALANCE,
                            groupType = TokensGroupType.NETWORK,
                        ),
                    ),
                ),
                ConvertModel(
                    value = createGetWalletAccountsResponse(
                        userWalletId = userWallet.walletId,
                        sortType = UserTokensResponse.SortType.MANUAL,
                        groupType = UserTokensResponse.GroupType.TOKEN,
                    ),
                    expected = Result.success(
                        createAccountList(
                            userWallet = userWallet,
                            sortType = TokensSortType.NONE,
                            groupType = TokensGroupType.NONE,
                        ),
                    ),
                ),
                ConvertModel(
                    value = createGetWalletAccountsResponse(
                        userWalletId = userWallet.walletId,
                        sortType = UserTokensResponse.SortType.MARKETCAP,
                        groupType = UserTokensResponse.GroupType.NONE,
                    ),
                    expected = Result.success(
                        createAccountList(
                            userWallet = userWallet,
                            sortType = TokensSortType.NONE,
                            groupType = TokensGroupType.NONE,
                        ),
                    ),
                ),
                ConvertModel(
                    value = GetWalletAccountsResponse(
                        wallet = GetWalletAccountsResponse.Wallet(
                            version = 0,
                            group = UserTokensResponse.GroupType.NETWORK,
                            sort = UserTokensResponse.SortType.BALANCE,
                            totalAccounts = 1,
                        ),
                        accounts = emptyList(),
                        unassignedTokens = emptyList(),
                    ),
                    expected = Result.failure(
                        IllegalStateException(
                            "Failed to convert GetWalletAccountsResponse to AccountList: EmptyAccountsList: " +
                                "The accounts list cannot be empty",
                        ),
                    ),
                ),
            )
        }
    }

    data class ConvertModel(
        val value: GetWalletAccountsResponse,
        val expected: Result<AccountList>,
    )
}