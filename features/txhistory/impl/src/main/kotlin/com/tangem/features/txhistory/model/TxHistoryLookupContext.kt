package com.tangem.features.txhistory.model

import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Per-page lookup context for the tx-history converter.
 *
 *  - [ownAccountByAddress] / [walletInfoById] — address-keyed lookups for resolving counterparty owners
 *    in transfer subtitles ("to / from MY account / wallet").
 *  - [isAccountsModeEnabled] — toggles whether a resolved owner is rendered as account or wallet.
 */
internal data class TxHistoryLookupContext(
    val ownAccountByAddress: Map<String, Account.CryptoPortfolio>,
    val isAccountsModeEnabled: Boolean,
    val walletInfoById: Map<UserWalletId, WalletInfo>,
)

internal data class WalletInfo(val name: String, val deviceIconUM: DeviceIconUM)

/**
 * Flattens every crypto-portfolio account of every wallet into an `address -> account` map for the network identified
 * by [networkRawId]. Shared by the history list and the details screen to decide whether a transfer counterparty is one
 * of the user's own accounts/wallets.
 */
internal fun buildOwnAccountAddressMap(
    lists: List<AccountStatusList>,
    networkRawId: Network.RawID,
): Map<String, Account.CryptoPortfolio> {
    val map = mutableMapOf<String, Account.CryptoPortfolio>()
    lists.forEach { accountList ->
        accountList.accountStatuses
            .filterCryptoPortfolio()
            .forEach { status ->
                status.flattenCurrencies().forEach { currencyStatus ->
                    if (currencyStatus.currency.network.id.rawId != networkRawId) return@forEach
                    val address = currencyStatus.value.networkAddress?.defaultAddress?.value ?: return@forEach
                    map[address] = status.account
                }
            }
    }
    return map
}