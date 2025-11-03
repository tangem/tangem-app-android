package com.tangem.domain.yield.supply

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import java.math.BigInteger

fun Fee.fixFee(cryptoCurrency: CryptoCurrency, gasLimit: BigInteger): Fee = when (this) {
    is Fee.Ethereum.Legacy -> copy(
        gasLimit = gasLimit,
        amount = amount.copy(
            value = gasPrice.multiply(gasLimit)
                .toBigDecimal().movePointLeft(cryptoCurrency.decimals),
        ),
    )
    is Fee.Ethereum.EIP1559 -> copy(
        gasLimit = gasLimit,
        amount = amount.copy(
            value = maxFeePerGas.multiply(gasLimit)
                .toBigDecimal().movePointLeft(cryptoCurrency.decimals),
        ),
    )
    else -> this
}