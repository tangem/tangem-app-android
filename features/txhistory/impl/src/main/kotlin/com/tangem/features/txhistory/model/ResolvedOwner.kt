package com.tangem.features.txhistory.model

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.ExpressTx

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
 * The resolved owners of a swap's two legs, after the "nothing worth naming" rules are applied: either side is `null`
 * when its leg should read "You send" / "You receive" with no owner — a swap settled entirely within one own portfolio
 * (both sides `null`), or a single-wallet own leg. A leg that differs (cross-account, cross-wallet, or an external
 * counterparty) keeps its [ResolvedOwner]. Shared by the history list row (counterparty leg) and the details screen
 * (both legs) so both name the same owner.
 */
internal data class SwapLegOwners(val from: ResolvedOwner?, val to: ResolvedOwner?)

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
 * Resolves the (from, to) owners of a [swap]'s legs. Each leg's address is resolved on its own currency's network
 * ([resolveExpressLeg]); the pair is then reduced by [SwapLegOwners]' rules: a swap within one own portfolio names no
 * owner, and an own-wallet leg is dropped when the user has a single wallet.
 */
internal fun TxHistoryLookupContext.resolveSwapLegOwners(swap: ExpressTx.Swap): SwapLegOwners {
    val from = resolveExpressLeg(swap.tx.fromAddress, swap.tx.fromAsset.cryptoCurrency)
    val to = resolveExpressLeg(swap.tx.payoutAddress, swap.tx.toAsset.cryptoCurrency)
    return if (isSameOwnPortfolio(from, to)) {
        SwapLegOwners(from = null, to = null)
    } else {
        SwapLegOwners(from = from?.let(::dropSingleOwnWallet), to = to?.let(::dropSingleOwnWallet))
    }
}

/**
 * Resolves a single swap/onramp leg's [address] (on the leg currency's network) to its owner, ready to display: an
 * own-wallet leg is dropped when the user has a single wallet (nothing to disambiguate). `null` when there is no
 * address to resolve. Used for the onramp payout leg, which has no second own leg to compare against.
 */
internal fun TxHistoryLookupContext.resolveExpressLegOwner(
    address: String?,
    legCurrency: CryptoCurrency?,
): ResolvedOwner? = resolveExpressLeg(address, legCurrency)?.let(::dropSingleOwnWallet)

/**
 * Raw resolution of a swap/onramp leg's [address] (on the leg currency's network) to its owner, before any display
 * rule. `null` when there is no address to resolve (e.g. onramp fiat has no address).
 */
private fun TxHistoryLookupContext.resolveExpressLeg(address: String?, legCurrency: CryptoCurrency?): ResolvedOwner? {
    if (address == null) return null
    return resolveOwner(address, legCurrency?.network?.id?.rawId)
}

/**
 * True when both swap legs settle in the same own portfolio — the same account, or (in wallet mode) the same wallet.
 * Such a swap has no counterparty to name, so its legs read "You send" / "You receive"; legs that differ keep an owner.
 */
private fun isSameOwnPortfolio(from: ResolvedOwner?, to: ResolvedOwner?): Boolean = when {
    from is ResolvedOwner.OwnAccount && to is ResolvedOwner.OwnAccount ->
        from.account.accountId == to.account.accountId
    from is ResolvedOwner.OwnPaymentAccount && to is ResolvedOwner.OwnPaymentAccount ->
        from.account.accountId == to.account.accountId
    from is ResolvedOwner.OwnWallet && to is ResolvedOwner.OwnWallet ->
        from.userWalletId == to.userWalletId
    else -> false
}

/** Drops an own-wallet owner when the user has a single wallet — there is no other wallet to tell it apart from. */
private fun TxHistoryLookupContext.dropSingleOwnWallet(owner: ResolvedOwner): ResolvedOwner? = when {
    owner is ResolvedOwner.OwnWallet && walletInfoById.size <= 1 -> null
    else -> owner
}

/**

 * wallet derived it, while a confirmed tx from an indexer may report the same address in a different case (e.g. EIP-55
 * checksummed vs lowercase EVM). A differing-case variant of another valid address would fail its checksum, so the
 * case-insensitive fallback cannot mis-attribute an external counterparty.
 */
private fun Map<String, Account>.getByAddress(address: String): Account? =
    this[address] ?: entries.firstOrNull { it.key.equals(address, ignoreCase = true) }?.value