package com.tangem.domain.transaction.usecase.gasless

import com.tangem.blockchain.blockchains.tron.TronTransactionExtras
import com.tangem.blockchain.blockchains.tron.tokenmethods.TronApprovalTokenCallData
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.transaction.models.tron.TronGaslessApprove
import java.math.BigDecimal
import java.math.BigInteger

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

/**
 * The TRC-20 approval this transaction grants, or `null` when it is not one Tron gasless relays: the
 * service approves an exact amount only, so an unlimited approval stays on the native fee.
 */
internal fun TransactionData.tronGaslessApproveOrNull(): TronGaslessApprove? {
    val uncompiled = this as? TransactionData.Uncompiled ?: return null
    val token = (uncompiled.amount.type as? AmountType.Token)?.token ?: return null
    val callData = (uncompiled.extras as? TronTransactionExtras)?.callData as? TronApprovalTokenCallData
        ?: return null
    val amount = callData.amount?.toTronGaslessBaseUnits()?.takeIf { it > BigInteger.ZERO } ?: return null

    return TronGaslessApprove(
        fromAddress = uncompiled.sourceAddress,
        tokenContract = token.contractAddress,
        amount = amount,
        spenderAddress = callData.spenderAddress,
    )
}

/** Contract of the token the compensation is charged in: the transferred token or the approved one. */
internal fun TransactionData.tronGaslessFeeTokenContractOrNull(): String? =
    tronGaslessTokenContractOrNull() ?: tronGaslessApproveOrNull()?.tokenContract