package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyExitCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Whether a pending transaction of the wallet belongs to [currency]'s history. It has to land where its confirmed
 * counterpart will: a pending row in the wrong history is never deduplicated by hash against the API page, so it
 * lingers there while the right history shows nothing.
 *
 * An ERC-20 `approve` is denominated in the approved token — a zero amount of it — yet moves none of it, so the
 * explorer reports it in the coin's history alone: a token's history is built from Transfer events and an approval
 * emits none. It therefore belongs to the coin, whatever its own amount says. A Merkl rewards claim is built the same
 * way — a zero amount of the yield token — and moves none of it either, so it belongs to the coin too.
 *
 * A Yield Mode enter or exit is built in a yield amount, which names no contract address, yet it moves the token in or
 * out of the module, so the explorer reports it in the token's history — and so must the pending row be.
 */
internal fun TransactionData.Uncompiled.belongsToHistoryOf(currency: CryptoCurrency): Boolean {
    if (isApproval() || isMerklClaim()) return currency is CryptoCurrency.Coin

    return when (currency) {
        is CryptoCurrency.Coin -> amount.type is AmountType.Coin
        is CryptoCurrency.Token -> {
            val movedTokenContractAddress = contractAddress ?: yieldMovedTokenContractAddress()
            movedTokenContractAddress == currency.contractAddress
        }
    }
}

private fun TransactionData.Uncompiled.yieldMovedTokenContractAddress(): String? {
    val callData = (extras as? EthereumTransactionExtras)?.callData?.unwrapYieldModuleUpgrade()
    val isTokenMoved = callData is EthereumYieldSupplyEnterCallData || callData is EthereumYieldSupplyExitCallData
    return (amount.type as? AmountType.TokenYieldSupply)?.token?.contractAddress?.takeIf { isTokenMoved }
}

private fun TransactionData.Uncompiled.isApproval(): Boolean =
    (extras as? EthereumTransactionExtras)?.callData is ApprovalERC20TokenCallData

private fun TransactionData.Uncompiled.isMerklClaim(): Boolean =
    (extras as? EthereumTransactionExtras)?.callData?.unwrapYieldModuleUpgrade() is MerklClaimCallData