package com.tangem.data.account.converter

import arrow.core.getOrElse
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.converter.Converter

/**
 * Converts a [GetWalletAccountsResponse] to an [AccountList]
 *
 * @property userWallet                      the user wallet associated with the account list
 * @property responseCryptoCurrenciesFactory factory to create crypto currencies from response tokens
 *
[REDACTED_AUTHOR]
 */
internal class AccountListConverter(
    private val userWallet: UserWallet,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
) : Converter<GetWalletAccountsResponse, AccountList> {

    override fun convert(value: GetWalletAccountsResponse): AccountList {
        return AccountList(
            userWallet = userWallet,
            accounts = value.accounts.map(::toCryptoPortfolio).toSet(),
            totalAccounts = value.wallet.totalAccounts,
            sortType = value.wallet.sort.toDomain(),
            groupType = value.wallet.group.toDomain(),
        )
            .getOrElse {
                error("Failed to convert GetWalletAccountsResponse to AccountList: $it")
            }
    }

    private fun toCryptoPortfolio(model: WalletAccountDTO): Account.CryptoPortfolio {
        return Account.CryptoPortfolio(
            accountId = model.id.toAccountId(userWallet.walletId),
            accountName = model.name.toAccountName(),
            icon = model.toIcon(),
            derivationIndex = model.derivationIndex.toDerivationIndex(),
            cryptoCurrencies = responseCryptoCurrenciesFactory.createCurrencies(
                tokens = model.tokens ?: error("Tokens should not be null"),
                userWallet = userWallet,
            ).toSet(),
        )
    }

    private fun UserTokensResponse.SortType.toDomain(): TokensSortType {
        return when (this) {
            UserTokensResponse.SortType.BALANCE -> TokensSortType.BALANCE
            UserTokensResponse.SortType.MANUAL,
            UserTokensResponse.SortType.MARKETCAP,
            -> TokensSortType.NONE
        }
    }

    private fun UserTokensResponse.GroupType.toDomain(): TokensGroupType {
        return when (this) {
            UserTokensResponse.GroupType.NETWORK -> TokensGroupType.NETWORK
            UserTokensResponse.GroupType.NONE,
            UserTokensResponse.GroupType.TOKEN,
            -> TokensGroupType.NONE
        }
    }
}