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
class GetWalletAccountsResponseConverterTest {

    private val userWallet = mockk<UserWallet> {
        every { walletId } returns UserWalletId("011")
    }
    private val cryptoPortfolioConverterFactory = mockk<CryptoPortfolioConverter.Factory>()
    private val cryptoPortfolioConverter = mockk<CryptoPortfolioConverter>()
    private val converter = GetWalletAccountsResponseConverter(
        userWallet = userWallet,
        cryptoPortfolioConverterFactory = cryptoPortfolioConverterFactory,
    )

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
            val domain = createAccountList(userWallet = userWallet)
            val exception = IllegalStateException("Test exception")

            every { cryptoPortfolioConverter.convertBack(any()) } throws exception

            // Act
            val actual = runCatching { converter.convert(domain) }.exceptionOrNull()!!

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
                model.value.accounts.forEach { domain ->
                    val dto = model.expected.getOrNull()!!.accounts.firstOrNull { it.id == domain.accountId.value }

                    every { cryptoPortfolioConverter.convertBack(domain as Account.CryptoPortfolio) } returns dto!!
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
                    value = createAccountList(
                        userWallet = userWallet,
                        sortType = TokensSortType.BALANCE,
                        groupType = TokensGroupType.NETWORK,
                    ),
                    expected = Result.success(
                        createGetWalletAccountsResponse(
                            userWalletId = userWallet.walletId,
                            sortType = UserTokensResponse.SortType.BALANCE,
                            groupType = UserTokensResponse.GroupType.NETWORK,
                        ),
                    ),
                ),
                ConvertModel(
                    value = createAccountList(
                        userWallet = userWallet,
                        sortType = TokensSortType.NONE,
                        groupType = TokensGroupType.NONE,
                    ),
                    expected = Result.success(
                        createGetWalletAccountsResponse(
                            userWalletId = userWallet.walletId,
                            sortType = UserTokensResponse.SortType.MANUAL,
                            groupType = UserTokensResponse.GroupType.NONE,
                        ),
                    ),
                ),
            )
        }
    }

    data class ConvertModel(
        val value: AccountList,
        val expected: Result<GetWalletAccountsResponse>,
    )
}