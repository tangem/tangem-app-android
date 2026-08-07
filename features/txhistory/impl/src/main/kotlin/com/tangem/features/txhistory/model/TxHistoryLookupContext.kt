package com.tangem.features.txhistory.model

import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Lookup context for resolving a transfer/swap counterparty to one of the user's own portfolios, shared by the history
 * list and the details screen (both assembled by [TxHistoryOwnerLookupProducer]).
 *
 *  - [ownAccountByNetwork] / [walletInfoById] — `address -> account` maps per network (a swap's legs can sit on
 *    different networks) plus per-wallet display info, used to render "to / from MY account / wallet".
 *  - [isAccountsModeEnabled] — toggles whether a resolved owner is rendered as account or wallet.
 *
 * The address → owner resolution lives in `ResolvedOwner.kt`; the on-chain `TxInfo` reclassification in
 * `TxInfoOwnClassification.kt`.
 */
internal data class TxHistoryLookupContext(
    val ownAccountByNetwork: Map<Network.RawID, Map<String, Account>>,
    val isAccountsModeEnabled: Boolean,
    val walletInfoById: Map<UserWalletId, WalletInfo>,
)

internal data class WalletInfo(val name: String, val deviceIconUM: DeviceIconUM)

/**
 * Flattens every account of every wallet into `address -> account` maps keyed by [Network.RawID] (a swap's two legs can
 * sit on different networks). Used to decide whether a transfer counterparty is one of the user's own accounts/wallets.
 * Crypto-portfolio accounts contribute each currency's address; a Payment (Tangem Pay) account contributes its deposit
 * address, so a transfer to the user's own Tangem Pay account resolves as own — not as an external address.
 */
internal fun buildOwnAccountAddressMapAllNetworks(
    lists: List<AccountStatusList>,
): Map<Network.RawID, Map<String, Account>> {
    val map = mutableMapOf<Network.RawID, MutableMap<String, Account>>()
    lists.forEach { accountList ->
        accountList.accountStatuses.forEach { status ->
            when (status) {
                is AccountStatus.CryptoPortfolio -> status.flattenCurrencies().forEach { currencyStatus ->
                    val address = currencyStatus.value.networkAddress?.defaultAddress?.value ?: return@forEach
                    val rawId = currencyStatus.currency.network.id.rawId
                    map.getOrPut(rawId) { mutableMapOf() }[address] = status.account
                }
                is AccountStatus.Payment -> {
                    val currencyStatus = (status.value as? PaymentAccountStatusValue.Loaded)?.cryptoCurrencyStatus
                    val address = currencyStatus?.value?.networkAddress?.defaultAddress?.value
                    if (currencyStatus != null && address != null) {
                        val rawId = currencyStatus.currency.network.id.rawId
                        map.getOrPut(rawId) { mutableMapOf() }[address] = status.account
                    }
                }
                is AccountStatus.Virtual -> Unit
            }
        }
    }
    return map.mapValues { (_, addresses) -> addresses.toMap() }
}