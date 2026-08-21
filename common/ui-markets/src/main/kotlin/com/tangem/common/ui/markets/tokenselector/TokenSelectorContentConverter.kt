package com.tangem.common.ui.markets.tokenselector

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * A single portfolio holding of a token: which wallet and account it lives on, and its current status.
 */
data class TokenSelectorEntry(
    val wallet: UserWallet,
    val account: AccountStatus.CryptoPortfolio,
    val currencyStatus: CryptoCurrencyStatus,
)

/**
 * Converts a list of portfolio holdings into [TokenSelectorContentUM].
 *
 * Entries are grouped by wallet (wallet headers appear only when more than one wallet is present) and then by
 * account (account headers appear while the accounts mode is enabled or more than one account holds the token).
 * Callers own how the entries are collected and filtered; this converter only turns them into display state.
 */
class TokenSelectorContentConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val isAccountsModeEnabled: Boolean,
    private val resolveWalletDeviceIcon: (UserWallet) -> DeviceIconUM,
    private val onEntryClick: (TokenSelectorEntry) -> Unit,
) : Converter<List<TokenSelectorEntry>, TokenSelectorContentUM> {

    private val rowFactory = UserAssetRowFactory(appCurrency = appCurrency, isBalanceHidden = isBalanceHidden)

    override fun convert(value: List<TokenSelectorEntry>): TokenSelectorContentUM = TokenSelectorContentUM(
        sections = buildSections(value).toImmutableList(),
    )

    private fun buildSections(entries: List<TokenSelectorEntry>): List<TokenSelectorSectionUM> {
        val sections = mutableListOf<TokenSelectorSectionUM>()
        val byWallet = entries.groupBy { it.wallet.walletId }
        val shouldShowWalletHeaders = byWallet.size > 1

        for ((_, walletEntries) in byWallet) {
            if (shouldShowWalletHeaders) {
                val wallet = walletEntries.first().wallet
                sections.add(
                    TokenSelectorSectionUM.WalletHeader(
                        walletName = wallet.name,
                        deviceIcon = resolveWalletDeviceIcon(wallet),
                    ),
                )
            }

            val byAccount = walletEntries.groupBy { it.account.account.accountId }
            val shouldShowAccountHeaders = isAccountsModeEnabled || byAccount.size > 1

            for ((_, accountEntries) in byAccount) {
                val singles = accountEntries.map(::entryToSingle).toImmutableList()
                val accountHeader = if (shouldShowAccountHeaders) {
                    val first = accountEntries.first()
                    AccountHeaderData(
                        accountName = first.account.account.accountName
                            .toUM()
                            .value,
                        cryptoPortfolioIcon = first.account.account.icon,
                    )
                } else {
                    null
                }
                sections.add(
                    TokenSelectorSectionUM.TokenGroup(accountHeader = accountHeader, items = singles),
                )
            }
        }
        return sections
    }

    private fun entryToSingle(entry: TokenSelectorEntry): UserAssetItemUM.Single = rowFactory.create(
        id = "${entry.wallet.walletId.stringValue}_${entry.account.account.accountId.value}_" +
            entry.currencyStatus.currency.id.value,
        currencyStatus = entry.currencyStatus,
        onClick = { onEntryClick(entry) },
    )
}