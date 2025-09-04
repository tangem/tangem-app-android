package com.tangem.data.account.converter

import arrow.core.getOrElse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.converter.Converter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Converts a [GetWalletAccountsResponse] to an [AccountList] and vice versa
 *
 * @property userWallet                   the user wallet associated with the account list
 * @param cryptoPortfolioConverterFactory factory to create [CryptoPortfolioConverter] instances
 *
[REDACTED_AUTHOR]
 */
internal class AccountListConverter @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    cryptoPortfolioConverterFactory: CryptoPortfolioConverter.Factory,
) : Converter<GetWalletAccountsResponse, AccountList> {

    private val cryptoPortfolioConverter: CryptoPortfolioConverter by lazy {
        cryptoPortfolioConverterFactory.create(userWallet)
    }

    override fun convert(value: GetWalletAccountsResponse): AccountList {
        return AccountList(
            userWallet = userWallet,
            accounts = value.accounts.map(cryptoPortfolioConverter::convert).toSet(),
            totalAccounts = value.wallet.totalAccounts,
            sortType = TokensSortTypeConverter.convert(value.wallet.sort),
            groupType = TokensGroupTypeConverter.convert(value.wallet.group),
        )
            .getOrElse {
                error("Failed to convert GetWalletAccountsResponse to AccountList: $it")
            }
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): AccountListConverter
    }
}