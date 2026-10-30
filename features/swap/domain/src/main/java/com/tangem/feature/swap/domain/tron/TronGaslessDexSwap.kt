package com.tangem.feature.swap.domain.tron

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.tron.TronGaslessSwap
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.toStringWithRightOffset

internal fun ExpressTransactionModel.DEX.toTronGaslessSwapOrNull(soldToken: CryptoCurrency.Token): TronGaslessSwap? {
    val payload = tronSwapPayload() as? TronSwapPayload.ContractCall ?: return null
    if (payload.memo != null) return null
    if (txValue?.toBigIntegerOrNull()?.signum() != 0) return null

    return TronGaslessSwap(
        fromAddress = txFrom,
        tokenContract = soldToken.contractAddress,
        amount = fromAmount.toStringWithRightOffset().toBigInteger(),
        routerAddress = txTo,
        callDataHex = payload.callDataHex.removePrefix("0x").lowercase(),
        spenderAddress = allowanceContract ?: txTo,
    )
}