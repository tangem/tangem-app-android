package com.tangem.common.ui.markets.tokenselector

import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts portfolio holdings into token-selector sections, for callers that already hold
 * [UserAssetEntry]s rather than full wallet/account models.
 *
 * Grouping matches [TokenSelectorContentConverter]: by wallet, then by account. Every account group
 * carries its header; a wallet header appears only when more than one wallet matched, since with a
 * single wallet the name adds nothing.
 *
 * The sections can be rendered inline in any list via `tokenSelectorSectionItems`, not only inside the
 * token-selector bottom sheet.
 */
class UserAssetEntrySectionsConverter(
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val walletIcons: Map<UserWalletId, DeviceIconUM>,
    private val onEntryClick: (UserAssetEntry) -> Unit,
) : Converter<List<UserAssetEntry>, ImmutableList<TokenSelectorSectionUM>> {

    private val rowFactory = UserAssetRowFactory(appCurrency = appCurrency, isBalanceHidden = isBalanceHidden)

    override fun convert(value: List<UserAssetEntry>): ImmutableList<TokenSelectorSectionUM> {
        val sections = mutableListOf<TokenSelectorSectionUM>()
        val byWallet = value.groupBy { it.userWalletId }
        val shouldShowWalletHeaders = byWallet.size > 1

        for ((walletId, walletEntries) in byWallet) {
            if (shouldShowWalletHeaders) {
                sections.add(
                    TokenSelectorSectionUM.WalletHeader(
                        walletName = walletEntries.first().userWalletName,
                        deviceIcon = walletIcons[walletId] ?: DeviceIconUM.Stub(cardsCount = 1),
                    ),
                )
            }

            for ((_, accountEntries) in walletEntries.groupBy { it.accountId }) {
                val first = accountEntries.first()
                sections.add(
                    TokenSelectorSectionUM.TokenGroup(
                        accountHeader = AccountHeaderData(
                            accountName = first.accountName.toUM().value,
                            cryptoPortfolioIcon = first.accountIcon,
                        ),
                        items = accountEntries.map(::entryToSingle).toImmutableList(),
                    ),
                )
            }
        }
        return sections.toImmutableList()
    }

    private fun entryToSingle(entry: UserAssetEntry): UserAssetItemUM.Single = rowFactory.create(
        id = "${entry.userWalletId.stringValue}_${entry.accountId.value}_${entry.currencyStatus.currency.id.value}",
        currencyStatus = entry.currencyStatus,
        onClick = { onEntryClick(entry) },
    )
}