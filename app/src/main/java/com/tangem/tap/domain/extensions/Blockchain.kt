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

fun Blockchain.amountToCreateAccount(token: Token? = null): BigDecimal? {
    return when (this) {
        Blockchain.Stellar -> if (token?.symbol == NODL) BigDecimal(1.5) else BigDecimal.ONE
        Blockchain.XRP -> BigDecimal(10)
        else -> null
    }
}

fun Blockchain.minimalAmount(): BigDecimal {
    return 1.toBigDecimal().movePointLeft(decimals())
}

private const val NODL = "NODL"