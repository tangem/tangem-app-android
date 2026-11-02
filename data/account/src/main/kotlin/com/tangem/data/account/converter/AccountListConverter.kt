package com.tangem.data.account.converter

import arrow.core.getOrElse
import com.tangem.data.account.utils.isJoint
import com.tangem.data.account.utils.isPersonal
import com.tangem.data.common.account.SupportedAccountTypes
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
 * @property supportedAccountTypes      decides which rows turn into accounts
 *
[REDACTED_AUTHOR]
 */
internal class AccountListConverter @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    cryptoPortfolioConverterFactory: CryptoPortfolioConverter.Factory,
    private val supportedAccountTypes: SupportedAccountTypes,
) : Converter<GetWalletAccountsResponse, AccountList> {

    private val cryptoPortfolioConverter: CryptoPortfolioConverter = cryptoPortfolioConverterFactory.create(userWallet)

    /** `api/v2` is also used for collectibles, so joint rows can arrive while joint accounts are off */
    private val WalletAccountDTO.isShownPortfolio: Boolean
        get() = (isPersonal || isJoint) && supportedAccountTypes.isSupported(this)

    override fun convert(value: GetWalletAccountsResponse): AccountList {
        val sortType = value.wallet.sort?.let(TokensSortTypeConverter::convert) ?: TokensSortType.NONE
        val groupType = value.wallet.group?.let(TokensGroupTypeConverter::convert) ?: TokensGroupType.NONE
        val (portfolioRows, otherRows) = value.accounts.partition { it.isShownPortfolio }
        val accounts = portfolioRows.map(::convertAccount) + convertNonPortfolioAccounts(otherRows)

        return AccountList(
            userWalletId = userWallet.walletId,
            accounts = accounts,
            totalAccounts = value.wallet.totalAccounts,
            totalArchivedAccounts = value.wallet.totalArchivedAccounts,
            totalJointAccounts = if (supportedAccountTypes.isSupported(WalletAccountDTO.Type.JOINT)) {
                // A backend that does not report the counter yet cannot be policed by it: falling back to the rows it
                // did send keeps the list buildable instead of failing it over a field this response never had
                value.wallet.totalJointAccounts ?: accounts.count { account -> account is Account.Joint }
            } else {
                0
            },
            sortType = sortType,
            groupType = groupType,
        )
            .getOrElse {
                error("Failed to convert GetWalletAccountsResponse to AccountList: $it")
            }
    }

    /**

     * no tokens at all. The crypto converter refuses an account without them, and the refusal is not even loud — the
     * account list producer turns it into an endless retry, leaving the wallet screen dead with no visible cause.
     */
    private fun convertAccount(value: WalletAccountDTO): Account {
        return cryptoPortfolioConverter.convert(value.copy(tokens = value.tokens.orEmpty()))
    }

    private fun convertNonPortfolioAccounts(rows: List<WalletAccountDTO>): List<Account> =
        rows.mapNotNull { accountDTO ->
            when (accountDTO.type) {
                WalletAccountDTO.Type.COLLECTIBLES -> {
                    Account.Collectibles(userWalletId = userWallet.walletId)
                        .takeIf { supportedAccountTypes.isSupported(accountDTO) }
                }
                WalletAccountDTO.Type.CRYPTO,
                WalletAccountDTO.Type.JOINT,
                null,
                -> null
            }
        }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): AccountListConverter
    }
}