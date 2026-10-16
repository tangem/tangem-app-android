package com.tangem.domain.transaction.usecase.gasless

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import java.math.BigDecimal

/**
 * Contract of the token moved by this transaction, or `null` when Tron gasless cannot price it: the
 * backend re-signs a real token transfer, so a compiled tx, a coin transfer and a zero-amount tx (an
 * approval) are all outside what it quotes.
 */
internal fun TransactionData.tronGaslessTokenContractOrNull(): String? {
    val uncompiled = this as? TransactionData.Uncompiled ?: return null
    val amount = uncompiled.amount.value ?: return null
    if (amount <= BigDecimal.ZERO) return null

    return when (val type = uncompiled.amount.type) {
        is AmountType.Token -> type.token.contractAddress
        is AmountType.TokenYieldSupply -> type.token.contractAddress
        else -> null
    }
}