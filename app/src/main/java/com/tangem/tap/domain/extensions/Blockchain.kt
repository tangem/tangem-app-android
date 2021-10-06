package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import java.math.BigDecimal


fun Blockchain.isNoAccountError(exception: Throwable): Boolean {
    return when (this) {
        Blockchain.Stellar, Blockchain.XRP ->
            exception.message?.contains("Account not found") == true
        else -> false
    }
}

fun Blockchain.amountToCreateAccount(token: Token? = null): Double? {
    return when (this) {
        Blockchain.Stellar -> if (token?.symbol == NODL) 1.5 else 1.toDouble()
        Blockchain.XRP -> 10.toDouble()
        else -> null
    }
}

fun Blockchain.minimalAmount(): BigDecimal {
    return 1.toBigDecimal().movePointLeft(decimals())
}

private const val NODL = "NODL"