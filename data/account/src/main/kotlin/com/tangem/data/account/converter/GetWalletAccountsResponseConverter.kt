package com.tangem.data.account.converter

import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.converter.Converter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
[REDACTED_AUTHOR]
 */
internal class GetWalletAccountsResponseConverter @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    cryptoPortfolioConverterFactory: CryptoPortfolioConverter.Factory,
) : Converter<AccountList, GetWalletAccountsResponse> {

    private val cryptoPortfolioConverter: CryptoPortfolioConverter by lazy {
        cryptoPortfolioConverterFactory.create(userWallet)
    }

    override fun convert(value: AccountList): GetWalletAccountsResponse {
        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = TokensGroupTypeConverter.convertBack(value.groupType),
                sort = TokensSortTypeConverter.convertBack(value.sortType),
                totalAccounts = value.totalAccounts,
            ),
            accounts = value.accounts
                .filterIsInstance<Account.CryptoPortfolio>()
                .map(cryptoPortfolioConverter::convertBack),
            unassignedTokens = emptyList(),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): GetWalletAccountsResponseConverter
    }
}