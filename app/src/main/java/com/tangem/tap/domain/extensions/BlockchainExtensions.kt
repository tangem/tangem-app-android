package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import org.stellar.sdk.requests.ErrorResponse


fun Blockchain.isNoAccountError(exception: Throwable): Boolean {
    return when (this) {
        Blockchain.Stellar -> {
            (exception is ErrorResponse) && (exception.code == 404)
        }
        Blockchain.XRP -> exception.message?.contains("Account not found") == true
        else -> false
    }
}

fun Blockchain.amountToCreateAccount(token: Token? = null): Double? {
    return when (this) {
        Blockchain.Stellar ->  if (token?.symbol == NODL) 1.5 else 1.toDouble()
        Blockchain.XRP -> 20.toDouble()
        else -> null
    }
}

private const val NODL = "NODL"