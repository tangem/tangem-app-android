package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** What the Safe refunds the relayer in; `gasPrice` of a `SafeTx` is counted in its smallest units */
sealed interface SafeGasToken {

    val address: String

    fun gasPriceInTokenCurrency(coinGasPrice: BigInteger): BigInteger

    data object Coin : SafeGasToken {
        override val address: String = SafeTxGasBuilder.NATIVE_GAS_TOKEN

        override fun gasPriceInTokenCurrency(coinGasPrice: BigInteger): BigInteger = coinGasPrice
    }

    /**
     * @param nativeToTokenRate how many tokens one coin is worth
     */
    data class Token(
        override val address: String,
        val decimals: Int,
        val coinDecimals: Int,
        val nativeToTokenRate: BigDecimal,
    ) : SafeGasToken {

        /** Rounded up: the refund is an integer per unit of gas and must never come out short */
        override fun gasPriceInTokenCurrency(coinGasPrice: BigInteger): BigInteger = coinGasPrice.toBigDecimal()
            .movePointLeft(coinDecimals)
            .multiply(nativeToTokenRate)
            .movePointRight(decimals)
            .setScale(0, RoundingMode.UP)
            .toBigInteger()
    }
}