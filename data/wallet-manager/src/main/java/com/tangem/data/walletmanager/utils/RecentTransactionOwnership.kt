package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Whether a pending transaction of the wallet belongs to [currency]'s history. It has to land where its confirmed
 * counterpart will: a pending row in the wrong history is never deduplicated by hash against the API page, so it
 * lingers there while the right history shows nothing.
 *
 * An ERC-20 `approve` is denominated in the approved token — a zero amount of it — yet moves none of it, so the
 * explorer reports it in the coin's history alone: a token's history is built from Transfer events and an approval
 * emits none. It therefore belongs to the coin, whatever its own amount says.
 */
internal fun TransactionData.Uncompiled.belongsToHistoryOf(currency: CryptoCurrency): Boolean {
    if (isApproval()) return currency is CryptoCurrency.Coin

    return when (currency) {
        is CryptoCurrency.Coin -> amount.type is AmountType.Coin
        is CryptoCurrency.Token -> contractAddress == currency.contractAddress
    }
}

private fun TransactionData.Uncompiled.isApproval(): Boolean =
    (extras as? EthereumTransactionExtras)?.callData is ApprovalERC20TokenCallData