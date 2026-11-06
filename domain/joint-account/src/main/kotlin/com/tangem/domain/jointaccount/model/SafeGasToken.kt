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

    /** A Smart Gas token known without its rate: only for a check that leaves the gas price ceiling alone */
    data class Unpriced(override val address: String) : SafeGasToken {

        override fun gasPriceInTokenCurrency(coinGasPrice: BigInteger): BigInteger =
            error("The gas token $address is not priced, the gas price ceiling cannot be computed")
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