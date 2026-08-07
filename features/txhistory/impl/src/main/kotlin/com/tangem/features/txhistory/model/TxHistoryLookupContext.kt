package com.tangem.features.txhistory.model

import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Lookup context for resolving a transfer/swap counterparty to one of the user's own portfolios, shared by the history
 * list and the details screen (both assembled by [TxHistoryOwnerLookupProducer]).
 *
 *  - [ownAccountByNetwork] / [walletInfoById] — `address -> account` maps per network (a swap's legs can sit on
 *    different networks) plus per-wallet display info, used to render "to / from MY account / wallet".
 *  - [isAccountsModeEnabled] — toggles whether a resolved owner is rendered as account or wallet.
 */
internal data class TxHistoryLookupContext(
    val ownAccountByNetwork: Map<Network.RawID, Map<String, Account.CryptoPortfolio>>,
    val isAccountsModeEnabled: Boolean,
    val walletInfoById: Map<UserWalletId, WalletInfo>,
)

internal data class WalletInfo(val name: String, val deviceIconUM: DeviceIconUM)

/**
 * The owner that a transfer counterparty resolves to, before it is mapped to a UI model. Shared by the history list
 * (subtitle) and the details screen (leg owner) so both apply the same precedence:
 * account (accounts mode on) → wallet (accounts mode off) → external address.
 */
internal sealed interface ResolvedOwner {
    data class OwnAccount(val account: Account.CryptoPortfolio) : ResolvedOwner
    data class OwnWallet(val walletInfo: WalletInfo) : ResolvedOwner
    data class External(val address: String) : ResolvedOwner
}

/**
 * Resolves a counterparty [address] on the network [networkRawId] to a [ResolvedOwner]: the owning account in accounts
 * mode, otherwise the owning wallet, falling back to the external address when it is none of the user's (or accounts
 * mode is off and the wallet info is missing).
 *
 * [networkRawId] `null` (an unresolved express leg whose `cryptoCurrency` is missing) falls back to a cross-network
 * lookup: the address is matched across every network and accepted only when it maps to exactly one account (EVM-family
 * addresses repeat across chains but stay within one account; a tie across distinct accounts stays external).
 */
internal fun TxHistoryLookupContext.resolveOwner(address: String, networkRawId: Network.RawID?): ResolvedOwner {
    val account = if (networkRawId != null) {
        ownAccountByNetwork[networkRawId]?.get(address)
    } else {
        ownAccountByNetwork.values
            .mapNotNull { it[address] }
            .distinctBy { it.accountId }
            .singleOrNull()
    }
    return when {
        account == null -> ResolvedOwner.External(address)
        isAccountsModeEnabled -> ResolvedOwner.OwnAccount(account)
        else -> walletInfoById[account.accountId.userWalletId]
            ?.let { ResolvedOwner.OwnWallet(it) }
            ?: ResolvedOwner.External(address)
    }
}

/**
 * Flattens every crypto-portfolio account of every wallet into `address -> account` maps keyed by [Network.RawID]
 * (a swap's two legs can sit on different networks). Used to decide whether a transfer counterparty is one of the
 * user's own accounts/wallets.
 */
internal fun buildOwnAccountAddressMapAllNetworks(
    lists: List<AccountStatusList>,
): Map<Network.RawID, Map<String, Account.CryptoPortfolio>> {
    val map = mutableMapOf<Network.RawID, MutableMap<String, Account.CryptoPortfolio>>()
    lists.forEach { accountList ->
        accountList.accountStatuses
            .filterCryptoPortfolio()
            .forEach { status ->
                status.flattenCurrencies().forEach { currencyStatus ->
                    val address = currencyStatus.value.networkAddress?.defaultAddress?.value ?: return@forEach
                    val rawId = currencyStatus.currency.network.id.rawId
                    map.getOrPut(rawId) { mutableMapOf() }[address] = status.account
                }
            }
    }
    return map.mapValues { (_, addresses) -> addresses.toMap() }
}