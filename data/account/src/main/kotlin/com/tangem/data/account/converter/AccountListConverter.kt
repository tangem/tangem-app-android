package com.tangem.data.account.converter

import arrow.core.getOrElse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
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
    private val jointAccountConverterFactory: JointAccountConverter.Factory,
) : Converter<GetWalletAccountsResponse, AccountList> {

    private val cryptoPortfolioConverter: CryptoPortfolioConverter by lazy {
        cryptoPortfolioConverterFactory.create(userWallet)
    }

    private val jointAccountConverter: JointAccountConverter by lazy {
        jointAccountConverterFactory.create(userWallet)
    }

    override fun convert(value: GetWalletAccountsResponse): AccountList {
        val sortType = value.wallet.sort?.let(TokensSortTypeConverter::convert) ?: TokensSortType.NONE
        val groupType = value.wallet.group?.let(TokensGroupTypeConverter::convert) ?: TokensGroupType.NONE

        return AccountList(
            userWalletId = userWallet.walletId,
            accounts = value.accounts.map(::convertAccount),
            totalAccounts = value.wallet.totalAccounts,
            totalArchivedAccounts = value.wallet.totalArchivedAccounts,
            sortType = sortType,
            groupType = groupType,
        )
            .getOrElse {
                error("Failed to convert GetWalletAccountsResponse to AccountList: $it")
            }
    }

    /**
     * A row whose `type` this build does not know degrades to a crypto account: the field is raw on purpose, and a
     * value the backend adds later must not cost the user the whole account list.
     *
     * `tokens` is normalised for the same reason. The crypto converter refuses a row without them, and such a row
     * is exactly what an unknown type may legitimately look like — while the refusal is not even loud: the account
     * list producer turns it into an endless retry, leaving the wallet screen dead with no visible cause.
     */
    private fun convertAccount(value: WalletAccountDTO): Account {
        return when (value.type) {
            WalletAccountDTO.TYPE_JOINT -> jointAccountConverter.convert(value)
            else -> cryptoPortfolioConverter.convert(value.copy(tokens = value.tokens.orEmpty()))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): AccountListConverter
    }
}