package com.tangem.features.feed.model.search.state.transformers

import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.markets.tokenselector.AccountHeaderData
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM
import com.tangem.common.ui.markets.tokenselector.TokenSelectorSectionUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.toImmutableList

internal class BuildTokenSelectorSectionsTransformer(
    private val entries: List<UserAssetEntry>,
    private val appCurrency: AppCurrency,
    private val isBalanceHidden: Boolean,
    private val walletIcons: Map<UserWalletId, DeviceIconUM>,
    private val onTokenSelected: (UserAssetEntry) -> Unit,
) : TokenSelectorUMTransformer {

    private val entryConverter = TokenSelectorEntryConverter(
        appCurrency = appCurrency,
        isBalanceHidden = isBalanceHidden,
        onTokenSelected = onTokenSelected,
    )

    override fun transform(prevState: TokenSelectorContentUM): TokenSelectorContentUM {
        return TokenSelectorContentUM(sections = buildSections().toImmutableList())
    }

    private fun buildSections(): List<TokenSelectorSectionUM> {
        val sections = mutableListOf<TokenSelectorSectionUM>()
        val byWallet = entries.groupBy { it.userWalletId }
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

            val byAccount = walletEntries.groupBy { it.accountId }
            val shouldShowAccountHeaders = byAccount.size > 1

            for ((_, accountEntries) in byAccount) {
                val singles = entryConverter.convertList(accountEntries).toImmutableList()
                val accountHeader = if (shouldShowAccountHeaders) {
                    val firstEntry = accountEntries.first()
                    AccountHeaderData(
                        accountName = firstEntry.accountName.toUM().value,
                        cryptoPortfolioIcon = firstEntry.accountIcon,
                    )
                } else {
                    null
                }
                sections.add(
                    TokenSelectorSectionUM.TokenGroup(
                        accountHeader = accountHeader,
                        items = singles,
                    ),
                )
            }
        }

        return sections
    }
}