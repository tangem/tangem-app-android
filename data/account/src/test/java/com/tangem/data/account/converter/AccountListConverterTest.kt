package com.tangem.data.account.converter

import com.google.common.truth.Truth
import com.tangem.common.test.utils.ProvideTestModels
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountListConverterTest {

    private val userWallet = mockk<UserWallet> {
        every { walletId } returns UserWalletId("011")
    }
    private val responseCryptoCurrenciesFactory = mockk<ResponseCryptoCurrenciesFactory>(relaxed = true)
    private val converter = AccountListConverter(userWallet, responseCryptoCurrenciesFactory)

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: TestModel) {
        // Act
        val actual = runCatching { converter.convert(model.value) }

        // Asset
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
                value = createDTO(
                    sortType = UserTokensResponse.SortType.BALANCE,
                    groupType = UserTokensResponse.GroupType.NETWORK,
                ),
                expected = Result.success(
                    createDomain(
                        sortType = TokensSortType.BALANCE,
                        groupType = TokensGroupType.NETWORK,
                    ),
                ),
            ),
            TestModel(
                value = createDTO(
                    sortType = UserTokensResponse.SortType.MANUAL,
                    groupType = UserTokensResponse.GroupType.TOKEN,
                ),
                expected = Result.success(
                    createDomain(
                        sortType = TokensSortType.NONE,
                        groupType = TokensGroupType.NONE,
                    ),
                ),
            ),
            TestModel(
                value = createDTO(
                    sortType = UserTokensResponse.SortType.MARKETCAP,
                    groupType = UserTokensResponse.GroupType.NONE,
                ),
                expected = Result.success(
                    createDomain(
                        sortType = TokensSortType.NONE,
                        groupType = TokensGroupType.NONE,
                    ),
                ),
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
                value = createDTO(accountName = ""),
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
                value = createDTO(tokens = null),
                expected = Result.failure(
                    IllegalStateException("Tokens should not be null"),
                ),
            ),
            TestModel(
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

    private fun createDTO(
        groupType: UserTokensResponse.GroupType = UserTokensResponse.GroupType.NETWORK,
        sortType: UserTokensResponse.SortType = UserTokensResponse.SortType.BALANCE,
        accountId: String? = null,
        accountName: String? = null,
        icon: String? = null,
        iconColor: String? = null,
        derivationIndex: Int? = null,
        tokens: List<UserTokensResponse.Token>? = emptyList(),
    ): GetWalletAccountsResponse {
        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                version = 0,
                group = groupType,
                sort = sortType,
                totalAccounts = 1,
            ),
            accounts = buildList {
                val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId = userWallet.walletId)

                WalletAccountDTO(
                    id = accountId ?: mainAccount.accountId.value,
                    name = accountName ?: mainAccount.accountName.value,
                    derivationIndex = derivationIndex ?: mainAccount.derivationIndex.value,
                    icon = icon ?: mainAccount.icon.value.name,
                    iconColor = iconColor ?: mainAccount.icon.color.name,
                    tokens = tokens,
                )
                    .let(::add)
            },
            unassignedTokens = emptyList(),
        )
    }

    private fun createDomain(
        sortType: TokensSortType = TokensSortType.BALANCE,
        groupType: TokensGroupType = TokensGroupType.NETWORK,
    ): AccountList {
        val mainAccount = Account.CryptoPortfolio.createMainAccount(userWalletId = userWallet.walletId)

        return AccountList(
            userWallet = userWallet,
            accounts = setOf(mainAccount),
            totalAccounts = 1,
            sortType = sortType,
            groupType = groupType,
        )
            .getOrNull()!!
    }

    data class TestModel(
        val value: GetWalletAccountsResponse,
        val expected: Result<AccountList>,
    )
}