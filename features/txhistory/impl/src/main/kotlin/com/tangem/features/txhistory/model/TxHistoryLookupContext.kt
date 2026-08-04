package com.tangem.features.txhistory.model

import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
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
    val ownAccountByNetwork: Map<Network.RawID, Map<String, Account>>,
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
    data class OwnPaymentAccount(val account: Account.Payment) : ResolvedOwner
    data class OwnWallet(val userWalletId: UserWalletId, val walletInfo: WalletInfo) : ResolvedOwner
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
        ownAccountByNetwork[networkRawId]?.getByAddress(address)
    } else {
        ownAccountByNetwork.values
            .mapNotNull { it.getByAddress(address) }
            .distinctBy { it.accountId }
            .singleOrNull()
    }
    return when {
        account == null -> ResolvedOwner.External(address)
        !isAccountsModeEnabled -> {
            val userWalletId = account.accountId.userWalletId
            walletInfoById[userWalletId]
                ?.let { ResolvedOwner.OwnWallet(userWalletId, it) }
                ?: ResolvedOwner.External(address)
        }
        account is Account.CryptoPortfolio -> ResolvedOwner.OwnAccount(account)
        account is Account.Payment -> ResolvedOwner.OwnPaymentAccount(account)
        else -> ResolvedOwner.External(address)
    }
}

/**
 * Direction-correct counterparty of a transaction: the recipient for an outgoing tx, the sender for an incoming one.
 *
 * Deliberately does not go through [TxInfo.interactionAddressType]: for an unrecognized contract call (Operation /
 * UnknownOperation) that field is always the destination (see `SdkTransactionHistoryItemConverter`), so on an incoming
 * tx it points at the viewed wallet itself. Picking the side that is not the viewed wallet lets the own-portfolio
 * lookup check the real other party. `null` for multi-address sides, where a single counterparty cannot be pinned.
 */
internal fun TxInfo.counterpartyAddress(): String? = if (isOutgoing) {
    (destinationType as? TxInfo.DestinationType.Single)?.addressType?.address
} else {
    (sourceType as? TxInfo.SourceType.Single)?.address
}

/**
 * Reclassifies an unrecognized contract call (Operation / UnknownOperation) as a plain [TxInfo.TransactionType.Transfer]
 * when its direction-correct [counterpartyAddress] resolves to one of the user's own accounts/wallets — such a tx is
 * really a transfer between the user's own portfolios and should read as Send / Receive / Transfer, not as a raw
 * operation. [TxInfo.interactionAddressType] is normalized to that counterparty so the downstream transfer rendering
 * (title, counterparty card, subtitle) uses the correct side. Any other tx is returned unchanged.
 */
internal fun TxInfo.reclassifyOwnOperationAsTransfer(
    lookup: TxHistoryLookupContext,
    networkRawId: Network.RawID,
): TxInfo {
    val isUnrecognizedCall = type is TxInfo.TransactionType.Operation ||
        type is TxInfo.TransactionType.UnknownOperation
    if (!isUnrecognizedCall) return this

    val counterparty = counterpartyAddress() ?: return this
    val owner = lookup.resolveOwner(address = counterparty, networkRawId = networkRawId)
    val isOwn = owner is ResolvedOwner.OwnAccount ||
        owner is ResolvedOwner.OwnPaymentAccount ||
        owner is ResolvedOwner.OwnWallet
    return if (isOwn) {
        copy(
            type = TxInfo.TransactionType.Transfer,
            interactionAddressType = TxInfo.InteractionAddressType.User(counterparty),
        )
    } else {
        this
    }
}

/**

 * wallet derived it, while a confirmed tx from an indexer may report the same address in a different case (e.g. EIP-55
 * checksummed vs lowercase EVM). A differing-case variant of another valid address would fail its checksum, so the
 * case-insensitive fallback cannot mis-attribute an external counterparty.
 */
private fun Map<String, Account>.getByAddress(address: String): Account? =
    this[address] ?: entries.firstOrNull { it.key.equals(address, ignoreCase = true) }?.value

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